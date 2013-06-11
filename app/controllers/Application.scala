package controllers
 
import play._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._

abstract class PatternDBItem

case class StringObj(text: String) extends PatternDBItem
case class Rule(val id:String, val provider: String, val rule_class : String, val patterns: Seq[StringObj], val tags: Seq[StringObj]) extends PatternDBItem
case class RuleSet(val name: String, val id:String, val patterns: Seq[StringObj], val rules: Seq[Rule]) extends PatternDBItem
 
object Application extends Controller {    

    implicit val stringobjFormat = Json.format[StringObj]
    implicit val ruleFormat = Json.format[Rule]
    implicit val rulesetFormat = Json.format[RuleSet]

    def get_ruleset_names() : Seq[String]  =  (scala.xml.XML.loadFile("data/postfix.xml")  \ "ruleset") map ( rule => (rule \ "@name").toString() )

    def get_ruleset(ruleset_name : String) : scala.xml.Node = ((scala.xml.XML.loadFile("data/postfix.xml") \ "ruleset") find (rule => ((rule \ "@name").toString() == ruleset_name))).get

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

   def save_ruleset_impl(ruleset:RuleSet): String = {
       val xml = to_xml(ruleset)
       return save_ruleset_xml(xml, ruleset.name)
   }

   def save_ruleset_xml(ruleset_xml : scala.xml.Node, ruleset_name : String) : String = {
       val pdb = scala.xml.XML.loadFile("data/postfix.xml")
       val removeIt = new scala.xml.transform.RewriteRule {
          override def transform(n: scala.xml.Node): scala.xml.NodeSeq = n match {
              case e: scala.xml.Elem if (e \ "@name").text == ruleset_name => scala.xml.NodeSeq.Empty
              case n => n
            }
       }
       val new_set = (new scala.xml.transform.RuleTransformer(removeIt).transform(pdb)) \ "ruleset" ++ ruleset_xml
       scala.xml.XML.save("data/new_xml.xml", <patterndb>{new_set}</patterndb>)
       return "OK"
   }

    def index = Action { Ok( 
        views.html.main.render("Hello") 
    ) }
    

   def namelist = Action { Ok(
       Json.toJson(get_ruleset_names()) 
   ) }

   def ruleset(ruleset_name : String ) = Action { 
      Ok( Json.toJson(map_ruleset(get_ruleset(ruleset_name ))))
   }

   def save_ruleset(ruleset_name : String) = Action(parse.json) { request =>
      Ok(save_ruleset_impl(request.body.asOpt[RuleSet].get))
   }
}
