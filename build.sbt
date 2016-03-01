lazy val akkaClusterControl = project
  .copy(id = "akka-cluster-control")
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin, GitVersioning)

name := "akka-cluster-control"

libraryDependencies ++= Vector(
  Library.scalaCheck % "test"
)

initialCommands := """|import de.heikoseeberger.akka.cluster.control._
                      |""".stripMargin
