import sbt._
import Keys._
import play.Project._
 
object ApplicationBuild extends Build {
 
  val appName         = "pdbedit"
  val appVersion      = "0.1"
 
  val appDependencies = Nil
 
  val main = play.Project(
    appName, appVersion, appDependencies
  ) 
 
}
