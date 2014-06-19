import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import controllers.PatternDB

class PatternDBTest extends Specification {
    "open" should {
       "return empty patterndb elements when invalid file present" in {
           val tempfile = java.io.File.createTempFile("invalid","")
           scala.tools.nsc.io.File(tempfile.getAbsolutePath()).writeAll("hello world")
           PatternDB.open(tempfile.getAbsolutePath()) must equalTo (<patterndb></patterndb>)
       }
    }
}
