name := "akka-cluster-control-app"

libraryDependencies ++= Vector(
  Library.akkaLog4j,
  Library.constructrAkka,
  Library.constructrCoordinationEtcd,
  Library.log4jCore
)

initialCommands := """|import de.heikoseeberger.akkaclustercontrol.app._
                      |""".stripMargin

daemonUser.in(Docker) := "root"
maintainer.in(Docker) := "Heiko Seeberger"
version.in(Docker)    := "latest"
dockerBaseImage       := "java:8"
dockerExposedPorts    := Vector(2552, 8000)
dockerRepository      := Some("hseeberger")
