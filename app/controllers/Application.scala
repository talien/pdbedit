package controllers

import scala.util.{Try, Success, Failure}
import scala.xml.{Elem, Node, TopScope, Text, Null}
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
case class Value(name: String, value: String) extends PatternDBItem
case class Rule(
    val id:String, 
    val provider: String, 
    val rule_class : String, 
    val description : Option[String], 
    val patterns: Seq[StringObj], 
    val tags: Seq[StringObj],
    val values : Seq[Value]
) extends PatternDBItem

case class RuleSet(
    val name: String, 
    val id: String, 
    val url: Option[String], 
    val description: Option[String], 
    val patterns: Seq[StringObj], 
    val rules: Seq[Rule]
) extends PatternDBItem

object Logger {
    def log(msg : String) : Unit = {
         val writer = new FileWriter("logs/app.log", true)
         val timestamp = (new java.text.SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]")).format( new java.util.Date())
         writer.write(timestamp+" "+msg+"\n")
         writer.close()
    }
}

object RulesetConverter {

    def getOptionalStringItemFromNodes(nodes: Seq[scala.xml.Node]) : Option[String] =
        nodes map ( element => element.text) 
           match { 
             case Seq() => None 
             case Seq(item) => Some(item) 
           }

    def XMLToValue(value : scala.xml.Node) : Value = 
        Value(
          (value \ "@name").toString(),
          value.text
        )

    def XMLToRule(rule: scala.xml.Node) : Rule =
        Rule(
          (rule \ "@id").toString(),
          (rule \ "@provider").toString(),
          (rule \ "@class").toString(),
          getOptionalStringItemFromNodes(rule \ "description"),
          (rule \ "patterns" \ "pattern") map ( pattern => StringObj(pattern.text) ),
          (rule \ "tags" \ "tag") map (tag => StringObj(tag.text)),
          (rule \ "values" \ "value") map (value => XMLToValue(value))
        )

    def XMLToRuleset(ruleset:  scala.xml.Node) : RuleSet = RuleSet(
        (ruleset \ "@name").toString(),
        (ruleset \ "@id").toString(),
        getOptionalStringItemFromNodes(ruleset \ "url"),
        getOptionalStringItemFromNodes(ruleset \ "description"),
        { 
          lazy val v3_patterns = (ruleset \ "pattern") map ( pattern => StringObj(pattern.text) )
          lazy val v4_patterns = (ruleset \ "patterns" \ "pattern") map ( pattern => StringObj(pattern.text) )
          if (v3_patterns  == Seq())
              v4_patterns
             else
              v3_patterns
        }
        ,
        (ruleset \ "rules" \ "rule") map ( rule => XMLToRule(rule))
    )

   def optionalStringToXML(item: Option[String], tag : String) : scala.xml.Node =
      item match { 
         case Some(value) => Elem(null, tag, Null, TopScope, Text(value))
         case None => Elem(null, tag, Null, TopScope)
      }

   def toXML(item: PatternDBItem) : scala.xml.Node = {
       item match {
          case StringObj(text) =>
             scala.xml.Unparsed(text)
          case Rule(id, provider, rule_class, description, patterns, tags, values) =>
            <rule id={id} provider={provider} class={rule_class}>
             { optionalStringToXML(description, "description") }
             <patterns>
               { patterns.map( pattern => <pattern>{toXML(pattern)}</pattern> ) }
             </patterns>
             <tags>
               { tags.map( tag => <tag>{toXML(tag)}</tag> ) }
             </tags>
             <values>
               { values.map( value => toXML(value) ) }
             </values>
            </rule>
          case Value(name, value) =>
            <value name={name}>{value}</value>
          case RuleSet(name, id, url, description, patterns, rules) =>
            <ruleset id={id} name={name}>
            { optionalStringToXML(url, "url") }
            { optionalStringToXML(description, "description") }
            <patterns>
             { patterns.map( pattern => <pattern>{toXML(pattern)}</pattern> ) }
            </patterns>
            <rules>
              { rules.map( rule => toXML(rule) ) }
            </rules>
            </ruleset>
       }
   }

   def rulesetToXML(ruleset: RuleSet) : scala.xml.Node = toXML(ruleset)

}

object PatternDB {
    def open(filename: String) : scala.xml.Node = {
        try {
            scala.xml.XML.loadFile(filename)
        } catch {
            case e:org.xml.sax.SAXParseException => createEmptyXML
        }
    }

    def save(filename: String, xml : scala.xml.Node) : Unit =
            scala.xml.XML.save(filename, xml, "utf-8", true)

    def getRulesetNames(filename : String) : Seq[String]  =  (open(filename)  \ "ruleset") map ( rule => (rule \ "@name").toString() )

    def getRulesetXml(filename : String, ruleset_name : String) : Option[scala.xml.Node] = 
            (((open(filename)) \ "ruleset") find (rule => ((rule \ "@name").toString() == ruleset_name)))

    def getRuleset(filename : String, ruleset_name : String) : Option[RuleSet] = 
        getRulesetXml(filename, ruleset_name) match {
           case Some(ruleset) => Some(RulesetConverter.XMLToRuleset(ruleset))
           case None => None
        }


    def removeRulesetFromXML( patterndb: scala.xml.Node, ruleset_name: String) : Seq[scala.xml.Node] = {
        val removeIt = new scala.xml.transform.RewriteRule {
            override def transform(n: scala.xml.Node): scala.xml.NodeSeq = n match {
              case e: scala.xml.Elem if (e \ "@name").text == ruleset_name => scala.xml.NodeSeq.Empty
              case n => n
            }
        }
        (new scala.xml.transform.RuleTransformer(removeIt).transform(patterndb)) 
    }

    def removeRuleset(filename: String, ruleset_name: String) : String = {
        val pdb = open(filename)
        val new_set = removeRulesetFromXML(pdb, ruleset_name) \ "ruleset"
        Logger.log("Removing ruleset:"+ruleset_name+" from file:"+filename)
        save(filename, wrapRulesetsInPatternDBPrologue(new_set))
        return "OK"
    }

    def saveRulesetXml(filename : String, ruleset_xml : scala.xml.Node, ruleset_name : String) : String = {
        val pdb = open(filename)
        val new_set = removeRulesetFromXML(pdb, ruleset_name) \ "ruleset" ++ ruleset_xml
        save(filename, wrapRulesetsInPatternDBPrologue(new_set))
        return "OK"
    }


    def saveRuleset(filename : String, ruleset : RuleSet): String = {
        val xml = RulesetConverter.rulesetToXML(ruleset)
        Logger.log("Saving ruleset:"+ruleset.name+" to file:"+filename)
        return saveRulesetXml(filename, xml, ruleset.name)
    }

    def wrapRulesetsInPatternDBPrologue(rulesets: Seq[scala.xml.Node]) : scala.xml.Node =  { 
<patterndb version={"4"} pub_date={ (new java.text.SimpleDateFormat("yyyy-MM-dd")).format( new java.util.Date())}> 
{rulesets}
</patterndb> 
    }

    def validate(filename: String) : Try[Unit] = Try {
           Logger.log("Validating patterndb file: "+filename)
           val schemaLang = "http://www.w3.org/2001/XMLSchema"
           val factory = SchemaFactory.newInstance(schemaLang)
           val schema = factory.newSchema(new StreamSource("data/patterndb-4.xsd"))
           val validator = schema.newValidator()
           validator.validate(new StreamSource(filename))
       }

   def prettyPrint(filename: String) = (new scala.xml.PrettyPrinter(120,4)).format(open(filename))

   def createEmptyXML = wrapRulesetsInPatternDBPrologue(Seq())

   def createEmpty(filename : String) = save(filename,createEmptyXML)

}

object Application extends Controller {

    implicit val stringobjFormat = Json.format[StringObj]
    implicit val valueFormat = Json.format[Value]
    implicit val ruleFormat = Json.format[Rule]
    implicit val rulesetFormat = Json.format[RuleSet]


   def getXMLFileName(session_id: String) : String = "/tmp/pdbedit/" + session_id + "/pdb.xml"

   def getXMLFileFromRequest (request : Request[_]) : String = getXMLFileName(request.session.get("session" ).get) 

   def namelist = Action { request => Ok(
       Json.toJson(PatternDB.getRulesetNames(getXMLFileFromRequest(request))) 
   ) }

   def ruleset(ruleset_name : String ) = Action { request => 
      PatternDB.getRuleset(getXMLFileFromRequest(request),ruleset_name ) match {
         case Some(ruleset) => Ok(Json.toJson(ruleset))
         case None => NotFound
      }
   }

   def saveRuleset(ruleset_name : String) = Action(parse.json) { request =>
      Ok(PatternDB.saveRuleset(getXMLFileFromRequest(request) , request.body.asOpt[RuleSet].get))
   }

   def deleteRuleset(ruleset_name: String) = Action { request =>
      Ok(PatternDB.removeRuleset(getXMLFileFromRequest(request), ruleset_name))
   }

   def getSessionDirectoryFile(session_id: String): java.io.File = new File("/tmp/pdbedit/" + session_id)

   def makeSessionDirectory(session_id : String) : Unit = {
       val dir = getSessionDirectoryFile(session_id)
       if (!dir.exists())
       {
           dir.mkdirs();  
       }
   }


   def initSession = {
      val session_id = java.util.UUID.randomUUID().toString()
      makeSessionDirectory(session_id)
      session_id
   }

   def redirectAndFlashError(error : String) = Redirect(routes.Application.start).flashing("error" -> error)

   def upload = Action(parse.multipartFormData){ request =>    
         request.body.file("patterndb").map { patterndb =>
            val session_id = initSession
            patterndb.ref.moveTo(new File(getXMLFileName(session_id)),true)
            if (patterndb.filename == "" ) redirectAndFlashError("Missing file!")
            else {
                PatternDB.validate(getXMLFileName(session_id)) match {
                    case Failure(ex) => redirectAndFlashError("Not a valid patterndb XML:"+ex.getMessage())
                    case Success(_) => Redirect(routes.Application.index).withSession( "session" -> session_id )
                }
            }
         }.getOrElse { redirectAndFlashError("Missing file!") }  
   }

   def getCurrentPatternDBFile(filename: String) : String = {
       Logger.log("Patterndb file downloaded: "+filename)
       return PatternDB.prettyPrint(filename)
   }

   def download = Action { request =>
      Ok(scala.xml.Unparsed(getCurrentPatternDBFile(getXMLFileFromRequest(request))))
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

   def cleanupSessionDirectory(session_id: String) : Unit = {
      Logger.log("Cleaning up session "+session_id)
      val dir = getSessionDirectoryFile(session_id)
      if (dir != null)
      {
        if (dir.listFiles != null) dir.listFiles.foreach( file => file.delete)
        dir.delete
      }
   }

   def logout = Action { request => 
      request.session.get("session") map { 
          session => cleanupSessionDirectory(session) }
      Redirect(routes.Application.start).withNewSession
   }

   def newFile = Action {
      val session_id = initSession
      PatternDB.createEmpty(getXMLFileName(session_id))

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

   def buildAndGetQuery(query: String) = {
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
      val qb = buildAndGetQuery(query)
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
