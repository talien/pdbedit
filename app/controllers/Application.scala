package controllers
 
import play._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.io.File
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory
import javax.xml.validation.{Validator=>JValidator}
import org.xml.sax.SAXException

abstract class PatternDBItem

case class StringObj(text: String) extends PatternDBItem
case class Rule(val id:String, val provider: String, val rule_class : String, val patterns: Seq[StringObj], val tags: Seq[StringObj]) extends PatternDBItem
case class RuleSet(val name: String, val id:String, val patterns: Seq[StringObj], val rules: Seq[Rule]) extends PatternDBItem

object RulesetConverter {
    def xml_to_ruleset(ruleset:  scala.xml.Node) : RuleSet = RuleSet(
        (ruleset \ "@name").toString(),
        (ruleset \ "@id").toString(),
        (ruleset \ "pattern") map ( pattern =>
            StringObj(pattern.text) ),
        (ruleset \ "rules" \ "rule") map ( rule =>
            Rule(
                  (rule \ "@id").toString(),
                  (rule \ "@provider").toString(),
                  (rule \ "@class").toString(),
                  (rule \ "patterns" \ "pattern") map ( pattern =>
                     StringObj(pattern.text) ),
                  (rule \ "tags" \ "tag") map (tag => 
                     StringObj(tag.text)))
    ))

   def to_xml(item: PatternDBItem) : scala.xml.Node = {
       item match {
          case StringObj(text) =>
             scala.xml.Unparsed(text)
          case Rule(id, provider, rule_class, patterns, tags) =>
            <rule id={id} provider={provider} class={rule_class}>
             <patterns>
               { patterns.map( pattern => <pattern>{to_xml(pattern)}</pattern> ) }
             </patterns>
             <tags>
               { tags.map( tag => <tag>{to_xml(tag)}</tag> ) }
             </tags>
            </rule>
          case RuleSet(name, id, patterns, rules) =>
            <ruleset id={id} name={name}>
            <patterns>
             { patterns.map( pattern => <pattern>{to_xml(pattern)}</pattern> ) }
            </patterns>
            <rules>
              { rules.map( rule => to_xml(rule) ) }
            </rules>
            </ruleset>
       }
   }

   def ruleset_to_xml(ruleset: RuleSet) : scala.xml.Node = to_xml(ruleset)

}

object Application extends Controller {

    implicit val stringobjFormat = Json.format[StringObj]
    implicit val ruleFormat = Json.format[Rule]
    implicit val rulesetFormat = Json.format[RuleSet]

   def open_pdb_file(filename: String) : scala.xml.Node = {
       try {
          scala.xml.XML.loadFile(filename)
       } catch {
           case e:org.xml.sax.SAXParseException => <patterndb></patterndb>
       }
   }

   def get_ruleset_names(filename : String) : Seq[String]  =  (open_pdb_file(filename)  \ "ruleset") map ( rule => (rule \ "@name").toString() )

   def get_ruleset_xml_from_file(filename : String, ruleset_name : String) : scala.xml.Node = (((open_pdb_file(filename)) \ "ruleset") find (rule => ((rule \ "@name").toString() == ruleset_name))).get

   def remove_ruleset_from_xml( patterndb: scala.xml.Node, ruleset_name: String) : Seq[scala.xml.Node] = {
        val removeIt = new scala.xml.transform.RewriteRule {
          override def transform(n: scala.xml.Node): scala.xml.NodeSeq = n match {
              case e: scala.xml.Elem if (e \ "@name").text == ruleset_name => scala.xml.NodeSeq.Empty
              case n => n
            }
       }
       (new scala.xml.transform.RuleTransformer(removeIt).transform(patterndb)) 
   }

   def save_patterndb(filename: String, xml : scala.xml.Node) : Unit = {
       scala.xml.XML.save(filename, xml, "utf-8", true)
   }

   def wrap_rulesets_in_patterndb_prologue(rulesets: Seq[scala.xml.Node]) : scala.xml.Node =  { 
<patterndb version={"4"} pub_date={ (new java.text.SimpleDateFormat("yyyy-MM-dd")).format( new java.util.Date())}> 
{rulesets}
</patterndb> 
   }

   def save_ruleset_xml(filename : String, ruleset_xml : scala.xml.Node, ruleset_name : String) : String = {
       val pdb = open_pdb_file(filename)
       val new_set = remove_ruleset_from_xml(pdb, ruleset_name) \ "ruleset" ++ ruleset_xml
       save_patterndb(filename, wrap_rulesets_in_patterndb_prologue(new_set))
       return "OK"
   }

   def remove_ruleset(filename: String, ruleset_name: String) : String = {
       val pdb = open_pdb_file(filename)
       val new_set = remove_ruleset_from_xml(pdb, ruleset_name) \ "ruleset"
       save_patterndb(filename, wrap_rulesets_in_patterndb_prologue(new_set))
       return "OK"
   }

   def save_ruleset_impl(filename : String, ruleset : RuleSet): String = {
       val xml = RulesetConverter.ruleset_to_xml(ruleset)
       println("Saving "+ruleset.name)
       return save_ruleset_xml(filename, xml, ruleset.name)
   }

   def get_xml_file_name(session_id: String) : String = "/tmp/pdbedit/" + session_id + "/pdb.xml"

   def get_xml_file_from_request (request : Request[_]) : String = get_xml_file_name(request.session.get("session" ).get) 

   def namelist = Action { request => Ok(
       Json.toJson(get_ruleset_names(get_xml_file_from_request(request))) 
   ) }

   def ruleset(ruleset_name : String ) = Action { request => 
      Ok( Json.toJson(
            RulesetConverter.xml_to_ruleset(
                get_ruleset_xml_from_file(get_xml_file_from_request(request),ruleset_name )
            )
      ))
   }

   def save_ruleset(ruleset_name : String) = Action(parse.json) { request =>
      Ok(save_ruleset_impl(get_xml_file_from_request(request) , request.body.asOpt[RuleSet].get))
   }

   def delete_ruleset(ruleset_name: String) = Action { request =>
      Ok(remove_ruleset(get_xml_file_from_request(request), ruleset_name))
   }

   def get_session_directory_file(session_id: String): java.io.File = new File("/tmp/pdbedit/" + session_id)

   def make_session_directory(session_id : String) : Unit = {
       val dir = get_session_directory_file(session_id)
       if (!dir.exists())
       {
           dir.mkdirs();  
       }
   }

   def validate_xml(filename: String) : Tuple2[Boolean,String] = {
      try {
          val schemaLang = "http://www.w3.org/2001/XMLSchema"
          val factory = SchemaFactory.newInstance(schemaLang)
          val schema = factory.newSchema(new StreamSource("data/patterndb-4.xsd"))
          val validator = schema.newValidator()
          validator.validate(new StreamSource(filename))
      } catch {
          case ex: SAXException => return (false, ex.getMessage())
          case ex: Exception => return(false, "Unknown error")
      }
      (true, "")
   }

   def upload = Action(parse.multipartFormData){ request =>    
         request.body.file("patterndb").map { patterndb =>
            val session_id = java.util.UUID.randomUUID().toString()
            make_session_directory(session_id)
            patterndb.ref.moveTo(new File(get_xml_file_name(session_id)),true)
            if (patterndb.filename == "" ) Redirect(routes.Application.start).flashing("error" -> "Missing file")
            else {
            val (res, error_msg ) = validate_xml(get_xml_file_name(session_id))
            if (!res) Redirect(routes.Application.start).flashing("error" -> ("Not a valid patterndb XML:"+error_msg)) else
            Redirect(routes.Application.index).withSession( "session" -> session_id )
            }
         }.getOrElse {
             Redirect(routes.Application.start).flashing("error" -> "Missing file")
            }  
   }

   def download = Action { request =>
      Ok(scala.xml.Unparsed((new scala.xml.PrettyPrinter(120,4)).format(open_pdb_file(get_xml_file_from_request(request)))))
   }

   def start = Action { request => 
     request.session.get("session") map { session =>
       Redirect(routes.Application.index) } getOrElse {
       Ok(views.html.start.render("Hello",
            request.flash.get("error") map {message =>
              message} getOrElse {""}

        ) ) }
   }

   def index = Action { request => 
      request.session.get("session") map { session =>
       Ok(views.html.main.render("Hello")) } getOrElse {
         Redirect(routes.Application.start)
       }
    }

   def cleanup_session_directory(session_id: String) : Unit = {
      val dir = get_session_directory_file(session_id)
      dir.listFiles.foreach( file => file.delete)
      dir.delete
   }

   def logout = Action { request => 
      request.session.get("session") map { 
          session => cleanup_session_directory(session) 
          Redirect(routes.Application.start).withNewSession
      } getOrElse {
        Redirect(routes.Application.start).withNewSession 
      }
   }
 
 }
