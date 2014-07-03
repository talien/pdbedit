import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import controllers.{PatternDB, RulesetConverter, RuleSet}

class PatternDBTest extends Specification {
    "open" should {
       "return empty patterndb elements when invalid file present" in {
           val tempfile = java.io.File.createTempFile("invalid","")
           scala.tools.nsc.io.File(tempfile.getAbsolutePath()).writeAll("hello world")
           PatternDB.open(tempfile.getAbsolutePath()) must equalTo (PatternDB.create_empty_xml)
       }
    }

    "get_ruleset_names" should {
       "return with empty list one empty patterndb" in {
           PatternDB.create_empty("test_empty.xml")
           PatternDB.get_ruleset_names("test_empty.xml") must equalTo (Seq())
       }

       "return with one name if one ruleset added" in {
           PatternDB.create_empty("test_onerule.xml")
           PatternDB.save_ruleset("test_onerule.xml", RuleSet("test", "test", Seq(), Seq()))
           PatternDB.get_ruleset_names("test_onerule.xml") must equalTo (Seq("test"))
       }
    }

    "get_ruleset" should {
       "be able to load ruleset saved with save_ruleset" in {
           val ruleset = RuleSet("test", "test", Seq(), Seq())
           PatternDB.create_empty("test_onerule.xml")
           PatternDB.save_ruleset("test_onerule.xml", ruleset)
           PatternDB.get_ruleset("test_onerule.xml", "test") must equalTo (ruleset)
       }

       "return what? if ruleset is not present" in {
           val ruleset = RuleSet("test", "test", Seq(), Seq())
           PatternDB.create_empty("test_norule.xml")
           PatternDB.get_ruleset("test_norule.xml", "test") must equalTo (ruleset)
       }
    }


}
