name := "lift-mongo-polyembed"

organization := "eltimn"

version := "0.1-SNAPSHOT"

scalaVersion := "2.9.1"

resolvers += "Liftmodules repo" at "https://repository-liftmodules.forge.cloudbees.com/release"

{
  val liftVersion = "2.4"
  libraryDependencies ++= Seq(
    "net.liftweb" %% "lift-mongodb-record" % liftVersion,
    "net.liftmodules" %% "mongoauth" % (liftVersion+"-0.3"),
    "ch.qos.logback" % "logback-classic" % "1.0.0",
    "org.scalatest" %% "scalatest" % "1.6.1" % "test",
    "org.mortbay.jetty" % "jetty" % "6.1.22" % "container"
  )
}

scalacOptions += "-deprecation"

seq(lessSettings:_*)

(LessKeys.filter in (Compile, LessKeys.less)) := "*styles.less"

(LessKeys.mini in (Compile, LessKeys.less)) := true

seq(closureSettings:_*)

seq(webSettings :_*)

// add managed resources, where less and closure publish to, to the webapp
(webappResources in Compile) <+= (resourceManaged in Compile)
