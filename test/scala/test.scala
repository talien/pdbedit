import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import controllers.RulesetConverter

class PDBTest extends Specification {

   "RulesetConverter" should {
       "convert ruleset xml to RuleSet object" in {
           val ruleset_xml = 
             <ruleset id="ruleset_id" name="ruleset_name">
              <url>test_url</url>
              <description>test_desc</description>
              <patterns>
               <pattern>program_pattern</pattern>
              </patterns>
              <rules>
               <rule class="rule_class" id="rule_id" provider="rule_provider">
                 <patterns>
                  <pattern>rule_pattern1</pattern>
                  <pattern>rule_pattern2</pattern>
                 </patterns>
                 <tags>
                   <tag>tag1</tag>
                 </tags>
               </rule>
              </rules>
             </ruleset>
           val ruleset = RulesetConverter.XMLToRuleset(ruleset_xml)
           ruleset.name must equalTo("ruleset_name")
           ruleset.id must equalTo("ruleset_id")
           ruleset.patterns(0).text must equalTo("program_pattern")
           ruleset.url must equalTo("test_url")
           ruleset.description must equalTo("test_desc")
       }
       "convert v3 ruleset xml to RuleSet object" in {
           val ruleset_xml = 
            <ruleset id="ruleset_id" name="ruleset_name">
              <pattern>program_pattern</pattern>
            </ruleset>
           val ruleset = RulesetConverter.XMLToRuleset(ruleset_xml)
           ruleset.patterns(0).text must equalTo("program_pattern")

       }
   }
}

class EndToEnd extends Specification {

   "Start page" should {
     "redirect when clicked on Create new" in {
       running(TestServer(3333), HTMLUNIT) { browser =>
          browser.goTo("http://localhost:3333")
          browser.$("#create_new").click()
          browser.url must equalTo("http://localhost:3333/index")

       }
     }

     "be able to click on new item" in {
       running(TestServer(3333), FIREFOX) { browser =>
          browser.goTo("http://localhost:3333")
          browser.$("#create_new").click()
          browser.$("#add_rule").click()
          browser.$("#rulename").text("test")
          browser.$("#add_rule_ok").click()
          Thread.sleep(5000)
          browser.url must equalTo("http://localhost:3333/index#")
       }
     }
   }
}
