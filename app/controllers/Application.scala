package controllers
 
import play._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.io.File

abstract class PatternDBItem

case class StringObj(text: String) extends PatternDBItem
case class Rule(val id:String, val provider: String, val rule_class : String, val patterns: Seq[StringObj], val tags: Seq[StringObj]) extends PatternDBItem
case class RuleSet(val name: String, val id:String, val patterns: Seq[StringObj], val rules: Seq[Rule]) extends PatternDBItem
 
object Application extends Controller {    

    implicit val stringobjFormat = Json.format[StringObj]
    implicit val ruleFormat = Json.format[Rule]
    implicit val rulesetFormat = Json.format[RuleSet]

    def map_ruleset(ruleset:  scala.xml.Node) : RuleSet = RuleSet(
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

   def open_pdb_file(filename: String) : scala.xml.Node = {
       try {
          scala.xml.XML.loadFile(filename)
       } catch {
           case e:org.xml.sax.SAXParseException => <patterndb></patterndb>
       }
   }

   def get_ruleset_names(filename : String) : Seq[String]  =  (open_pdb_file(filename)  \ "ruleset") map ( rule => (rule \ "@name").toString() )

   def get_ruleset(filename : String, ruleset_name : String) : scala.xml.Node = (((open_pdb_file(filename)) \ "ruleset") find (rule => ((rule \ "@name").toString() == ruleset_name))).get

   def remove_ruleset_from_xml( patterndb: scala.xml.Node, ruleset_name: String) : Seq[scala.xml.Node] = {
        val removeIt = new scala.xml.transform.RewriteRule {
          override def transform(n: scala.xml.Node): scala.xml.NodeSeq = n match {
              case e: scala.xml.Elem if (e \ "@name").text == ruleset_name => scala.xml.NodeSeq.Empty
              case n => n
            }
       }
       (new scala.xml.transform.RuleTransformer(removeIt).transform(patterndb)) 
   }

   def save_ruleset_xml(filename : String, ruleset_xml : scala.xml.Node, ruleset_name : String) : String = {
       val pdb = open_pdb_file(filename)
       val new_set = remove_ruleset_from_xml(pdb, ruleset_name) \ "ruleset" ++ ruleset_xml
       scala.xml.XML.save(filename, <patterndb>{new_set}</patterndb>)
       return "OK"
   }

   def remove_ruleset(filename: String, ruleset_name: String) : String = {
       val pdb = open_pdb_file(filename)
       val new_set = remove_ruleset_from_xml(pdb, ruleset_name) \ "ruleset"
       scala.xml.XML.save(filename, <patterndb>{new_set}</patterndb>)
       return "OK"
   }

   def save_ruleset_impl(filename : String, ruleset : RuleSet): String = {
       val xml = to_xml(ruleset)
       println("Saving "+ruleset.name)
       return save_ruleset_xml(filename, xml, ruleset.name)
   }

   def get_xml_file_name(session_id: String) : String = "/tmp/pdbedit/" + session_id + "/pdb.xml"

   def get_xml_file_from_request (request : Request[_]) : String = get_xml_file_name(request.session.get("session" ).get) 

   def namelist = Action { request => Ok(
       Json.toJson(get_ruleset_names(get_xml_file_from_request(request))) 
   ) }

   def ruleset(ruleset_name : String ) = Action { request => 
      Ok( Json.toJson(map_ruleset(get_ruleset(get_xml_file_from_request(request),ruleset_name ))))
   }

   def save_ruleset(ruleset_name : String) = Action(parse.json) { request =>
      Ok(save_ruleset_impl(get_xml_file_from_request(request) , request.body.asOpt[RuleSet].get))
   }

   def delete_ruleset(ruleset_name: String) = Action { request =>
      Ok(remove_ruleset(get_xml_file_from_request(request), ruleset_name))
   }

   def make_session_directory(session_id : String) : Unit = {
       val dir = new File("/tmp/pdbedit/" + session_id)
       if (!dir.exists())
       {
           dir.mkdirs();  
       }
   }

   def upload = Action(parse.multipartFormData){ request =>    
     request.body.file("picture").map { picture =>
      val uuid = java.util.UUID.randomUUID().toString()
      make_session_directory(uuid)
      picture.ref.moveTo(new File(get_xml_file_name(uuid)),true)
      Redirect(routes.Application.index).withSession( "session" -> uuid )
    }.getOrElse {
        Redirect(routes.Application.start).flashing(
      "error" -> "Missing file")
   }  }

   def download = Action { request =>
      Ok(scala.xml.Unparsed((new scala.xml.PrettyPrinter(120,4)).format(open_pdb_file(get_xml_file_from_request(request)))))
   }

   def start = Action { request => 
     request.session.get("session") map { session =>
       Redirect(routes.Application.index) } getOrElse {
       Ok(views.html.start.render("Hello") ) }
   }

   def index = Action { request => 
      request.session.get("session") map { session =>
       Ok(views.html.main.render("Hello")) } getOrElse {
         Redirect(routes.Application.start)
       }
    }

   def logout = Action { Redirect(routes.Application.start).withNewSession }
 
}
