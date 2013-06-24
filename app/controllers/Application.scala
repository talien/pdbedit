package controllers
 
import play._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.io._
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory
import javax.xml.validation.{Validator=>JValidator}
import org.xml.sax.SAXException
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.index.query.FilterBuilders._
import org.elasticsearch.index.query.QueryBuilders._
import org.elasticsearch.node.NodeBuilder._
import collection.JavaConversions._
import org.elasticsearch.common.unit._

abstract class PatternDBItem

case class StringObj(text: String) extends PatternDBItem
case class Rule(val id:String, val provider: String, val rule_class : String, val patterns: Seq[StringObj], val tags: Seq[StringObj]) extends PatternDBItem
case class RuleSet(val name: String, val id:String, val patterns: Seq[StringObj], val rules: Seq[Rule]) extends PatternDBItem

object Logger {
    def log(msg : String) : Unit = {
         val writer = new FileWriter("logs/app.log", true)
         val timestamp = (new java.text.SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]")).format( new java.util.Date())
         writer.write(timestamp+" "+msg+"\n")
         writer.close()
    }
}

object RulesetConverter {
    def xml_to_ruleset(ruleset:  scala.xml.Node) : RuleSet = RuleSet(
        (ruleset \ "@name").toString(),
        (ruleset \ "@id").toString(),
        { 
          lazy val v3_patterns = (ruleset \ "pattern") map ( pattern => StringObj(pattern.text) )
          lazy val v4_patterns = (ruleset \ "patterns" \ "pattern") map ( pattern => StringObj(pattern.text) )
          if (v3_patterns  == Seq())
              v4_patterns
             else
              v3_patterns
        }
        ,
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
       Logger.log("Removing ruleset:"+ruleset_name+" from file:"+filename)
       save_patterndb(filename, wrap_rulesets_in_patterndb_prologue(new_set))
       return "OK"
   }

   def save_ruleset_impl(filename : String, ruleset : RuleSet): String = {
       val xml = RulesetConverter.ruleset_to_xml(ruleset)
       Logger.log("Saving ruleset:"+ruleset.name+" to file:"+filename)
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
          Logger.log("Validating patterndb file: "+filename)
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

   def init_session = {
      val session_id = java.util.UUID.randomUUID().toString()
      make_session_directory(session_id)
      session_id
   }

   def upload = Action(parse.multipartFormData){ request =>    
         request.body.file("patterndb").map { patterndb =>
            val session_id = init_session
            patterndb.ref.moveTo(new File(get_xml_file_name(session_id)),true)
            if (patterndb.filename == "" ) Redirect(routes.Application.start).flashing("error" -> "Missing file")
            else {
                val (res, error_msg ) = validate_xml(get_xml_file_name(session_id))
                if (!res) 
                    Redirect(routes.Application.start).flashing("error" -> ("Not a valid patterndb XML:"+error_msg)) 
                else
                    Redirect(routes.Application.index).withSession( "session" -> session_id )
            }
         }.getOrElse {
             Redirect(routes.Application.start).flashing("error" -> "Missing file")
            }  
   }

   def get_current_patterndb_file(filename: String) = {
       Logger.log("Patterndb file downloaded: "+filename)
       (new scala.xml.PrettyPrinter(120,4)).format(open_pdb_file(filename))
   }

   def download = Action { request =>
      Ok(scala.xml.Unparsed(get_current_patterndb_file(get_xml_file_from_request(request))))
   }

   def start = Action { request => 
     request.session.get("session") map { session =>
       Redirect(routes.Application.index) } getOrElse {
       Ok(Scalate("start.jade").render('msg -> 
            (request.flash.get("error") map {message =>
              message} getOrElse {""})
        ) ) }
   }

   def index = Action { request => 
      request.session.get("session") map { session =>
       Ok(Scalate("main.jade").render()) } getOrElse {
         Redirect(routes.Application.start)
       }
    }

   def cleanup_session_directory(session_id: String) : Unit = {
      Logger.log("Cleaning up session "+session_id)
      val dir = get_session_directory_file(session_id)
      dir.listFiles.foreach( file => file.delete)
      dir.delete
   }

   def logout = Action { request => 
      request.session.get("session") map { 
          session => cleanup_session_directory(session) }
      Redirect(routes.Application.start).withNewSession
   }

   def save_empty_patterndb(session_id : String) = save_patterndb(get_xml_file_name(session_id),wrap_rulesets_in_patterndb_prologue(Seq()))

   def new_file = Action {
      val session_id = init_session
      save_empty_patterndb(session_id)
      Logger.log("New file created in session "+session_id)
      Redirect(routes.Application.index).withSession( "session" -> session_id )
   }

   def searchmain = Action {
      Ok(Scalate("search.jade").render())
   }

/*   def get_scroll_id_for_search() = {
      val node = nodeBuilder().node();
      val client = node.client();
      val scrollResp = client.prepareSearch("test")
        .setSearchType(SearchType.SCAN)
        .setScroll(new TimeValue(60000))
        .setSize(50).execute().actionGet()
      val res = scrollResp.getScrollId()
      node.close()
      res
   }

   def get_values_from_scroll(scroll_id: String, from: Int) = {
       val node = nodeBuilder().node();
       val client = node.client();
       val response = client.prepareSearchScroll(scroll_id).setScroll(new TimeValue(600000)).execute().actionGet();
       val res = response.getHits().slice(0,50).map ( hit => {
          hit.getFields().foreach {
              field => println(field._1)
          }
          //println(hit.sourceAsString())
          Json.parse(hit.sourceAsString())
       })
       node.close()
       Json.toJson(res)
   }

   def elastictest(from: Int) = Action {
      request => request.session.get("scroll_id") map {
        scroll_id => 
        {
          println("Got scrollid:" + scroll_id)
          Ok(get_values_from_scroll(scroll_id, from))
        }
      } getOrElse { 
         val scrollid = get_scroll_id_for_search()
         Ok(get_values_from_scroll(scrollid, from)).withSession("scroll_id" -> scrollid)
      }
   }*/

   def build_and_get_query(query: String) = {
       if (query == "")
          matchAllQuery()
       else
          matchQuery("_all",query)
   }

   def elastictest(from: Int) = Action(parse.json) {
     request => {
      val query = (request.body \ "query").as[String]
      println(query)
      val node = nodeBuilder().node();
      val client = node.client();
      val qb = build_and_get_query(query)
      val response = client.prepareSearch("test").setFrom(from).setQuery(qb).setSize(50).execute().actionGet();
      println(response.getHits().totalHits())
      val res = response.getHits().slice(0,50).map ( hit => {
          hit.getFields().foreach {
              field => println(field._1)
          }
          //println(hit.sourceAsString())
          Json.parse(hit.sourceAsString())
      }
      )
      node.close();
      Ok(Json.toJson(res))
     }

   }
 
 }
