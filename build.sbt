name := "covid-19-akka-stream"

version := "0.1"

scalaVersion := "2.13.4"

idePackagePrefix := Some("com.github.nullptr7")

libraryDependencies ++= Seq (
  "com.typesafe.akka" %% "akka-stream" % "2.6.5",
  "com.typesafe.akka" %% "akka-http" % "10.2.3",
)
