import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import controllers.{PatternDB, RulesetConverter, RuleSet}

class PatternDBTest extends Specification {
    def testRuleSet = RuleSet("test", "test", "testUrl", "testDesc",Seq(), Seq())

    "open" should {
       "return empty patterndb elements when invalid file present" in {
           val tempfile = java.io.File.createTempFile("invalid","")
           scala.tools.nsc.io.File(tempfile.getAbsolutePath()).writeAll("hello world")
           PatternDB.open(tempfile.getAbsolutePath()) must equalTo (PatternDB.createEmptyXML)
       }
    }

    "get_ruleset_names" should {
       "return with empty list one empty patterndb" in {
           PatternDB.createEmpty("test_empty.xml")
           PatternDB.getRulesetNames("test_empty.xml") must equalTo (Seq())
       }

       "return with one name if one ruleset added" in {
           PatternDB.createEmpty("test_onerule.xml")
           PatternDB.saveRuleset("test_onerule.xml", testRuleSet)
           PatternDB.getRulesetNames("test_onerule.xml") must equalTo (Seq("test"))
       }
    }

    "get_ruleset" should {
       "be able to load ruleset saved with save_ruleset" in {
           PatternDB.createEmpty("test_onerule.xml")
           PatternDB.saveRuleset("test_onerule.xml", testRuleSet)
           PatternDB.getRuleset("test_onerule.xml", "test") must equalTo (Some(testRuleSet))
       }

       "return None if ruleset is not present" in {
           PatternDB.createEmpty("test_norule.xml")
           PatternDB.getRuleset("test_norule.xml", "test") must equalTo (None)
       }
    }


}
