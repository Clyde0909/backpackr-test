name := "backpackr-project"
version := "1.0"
scalaVersion := "2.12.15"  // Spark 3.5.0과 호환되는 Scala 버전

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-sql" % "3.5.0",
  "org.apache.spark" %% "spark-hive" % "3.5.0",
  "org.postgresql" % "postgresql" % "42.7.1"
)