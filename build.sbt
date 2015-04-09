import play.PlayImport.PlayKeys._

name := "m101j-blogs-play-framework"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.4"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

routesImport += "models._"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  javaWs,
  "org.mongodb" % "mongo-java-driver" % "3.0.0-rc0",
  "org.apache.commons" % "commons-lang3" % "3.1",
  "commons-codec" % "commons-codec" % "1.7"
)

resolvers ++= Seq(
  "Apache" at "http://repo1.maven.org/maven2/",
  "maven-central (release)" at "http://search.maven.org/"
)

initialize := {
  val _ = initialize.value
  if (sys.props("java.specification.version") != "1.8")
    sys.error("Java 8 is required for this project.")
}