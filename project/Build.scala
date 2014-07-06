import sbt._
import Keys._
import play.Project._
import play._
 
object ApplicationBuild extends Build {
 
  val appName         = "pdbedit"
  val appVersion      = "0.1"
 
  val appDependencies =  Seq("org.fusesource.scalate" %% "scalate-core" % "1.6.1", 
                             "org.elasticsearch" % "elasticsearch" % "0.90.1", 
                             "org.seleniumhq.selenium" % "selenium-java" % "2.41.0" )

  val coffeetestTask = TaskKey[Unit]("coffee-test-compile", "Compiling coffeescript tests for Jasmine")
  val coffeetest = (baseDirectory) map { (base) => 
      val files2 = base / "test"  ** "*.coffee"
      val jsfiles = files2.get map (coffeeFile => {
                (play.core.coffeescript.CoffeescriptCompiler.compile(coffeeFile, Seq("bare")), coffeeFile.getPath().replace(".coffee", ".js") )
                
      } )
      jsfiles foreach ( value => {
              val out = new File(value._2)
              IO.write(out, value._1)
              println("Compiled:" + value._2)
              } 
          ) 

  }
    
  val main = play.Project(
    appName, appVersion, appDependencies
  ).settings(
     coffeescriptOptions := Seq("bare")
  ).settings(
     coffeetestTask <<= coffeetest
  )
}

