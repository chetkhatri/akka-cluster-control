name := "akka-cluster-control-app"

libraryDependencies ++= Vector(
  Library.akkaLog4j,
  Library.constructrAkka,
  Library.constructrCoordinationEtcd,
  Library.log4jCore
)

initialCommands := """|import de.heikoseeberger.akkaclustercontrol.app._
                      |""".stripMargin

version.in(Docker)    := "latest"
maintainer.in(Docker) := "Heiko Seeberger"
daemonUser.in(Docker) := "root"
dockerBaseImage       := "java:8"
dockerRepository      := Some("hseeberger")
dockerExposedPorts    := Vector(2552, 8000)
