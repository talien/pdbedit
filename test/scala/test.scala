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
                 <url>rule_url</url>
                 <description>rule_desc</description>
                 <values>
                   <value name="value_name">value_value</value>
                 </values>
                 <examples>
                  <example>
                    <test_message program="example_program">example_message</test_message>
                    <test_values>
                      <test_value name="example_value_name">example_value_value</test_value>
                    </test_values>
                    <test_tags>
                      <test_tag>example_tag</test_tag>
                    </test_tags>
                  </example>
                 </examples>
               </rule>
              </rules>
             </ruleset>
           val ruleset = RulesetConverter.XMLToRuleset(ruleset_xml)
           ruleset.name must equalTo("ruleset_name")
           ruleset.id must equalTo("ruleset_id")
           ruleset.patterns(0).text must equalTo("program_pattern")
           ruleset.url must equalTo(Some("test_url"))
           ruleset.description must equalTo(Some("test_desc"))
           val rule = ruleset.rules.head
           rule.rule_class must equalTo ("rule_class")
           rule.id must equalTo ("rule_id")
           rule.provider must equalTo ("rule_provider")
           rule.patterns.head.text must equalTo ("rule_pattern1")
           rule.values.head.name must equalTo ("value_name")
           rule.values.head.value must equalTo ("value_value")
           val example = rule.examples.head
           example.test_message must equalTo ("example_message")
           example.test_program must equalTo ("example_program")
           example.test_values.head.name must equalTo ("example_value_name")
           example.test_values.head.value must equalTo ("example_value_value")
           example.test_tags.head.text must equalTo ("example_tag")
           RulesetConverter.XMLToRuleset(RulesetConverter.toXML(ruleset)) must equalTo (ruleset)
           
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
       running(TestServer(3333), HTMLUNIT) { browser =>
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
