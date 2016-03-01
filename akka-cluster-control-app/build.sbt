name := "akka-cluster-control-app"

libraryDependencies ++= Vector(
  Library.akkaLog4j,
  Library.log4jCore
)

initialCommands := """|import de.heikoseeberger.akkaclustercontrol.app._
                      |""".stripMargin
