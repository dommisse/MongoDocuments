name := "DocMan"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies ++= Seq("org.mongodb.scala" % "mongo-scala-driver_2.12" % "1.2.1",
  "org.mongodb.scala" % "mongo-scala-bson_2.12" % "1.2.1"
)
