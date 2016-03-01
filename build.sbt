lazy val root = project
  .copy(id = "root")
  .in(file("."))
  .enablePlugins(GitVersioning)
  .aggregate(akkaClusterControl, akkaClusterControlApp)

lazy val akkaClusterControl = project
  .copy(id = "akka-cluster-control")
  .in(file("akka-cluster-control"))
  .enablePlugins(AutomateHeaderPlugin)
  .configs(MultiJvm)

lazy val akkaClusterControlApp = project
  .copy(id = "akka-cluster-control-app")
  .in(file("akka-cluster-control-app"))
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(akkaClusterControl)

name := "root"

unmanagedSourceDirectories in Compile := Vector.empty
unmanagedSourceDirectories in Test    := Vector.empty

publishArtifact := false
