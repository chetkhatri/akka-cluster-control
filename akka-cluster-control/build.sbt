name := "akka-cluster-control"

libraryDependencies ++= Vector(
  Library.akkaCluster,
  Library.akkaHttp,
  Library.akkaHttpCirce,
  Library.akkaSse,
  Library.circeGeneric,
  Library.akkaHttpTestkit      % "test",
  Library.akkaMultiNodeTestkit % "test",
  Library.akkaTestkit          % "test",
  Library.scalaTest            % "test"
)

initialCommands := """|import de.heikoseeberger.akkaclustercontrol._
                      |""".stripMargin

unmanagedSourceDirectories.in(MultiJvm) := Vector(scalaSource.in(MultiJvm).value)
test.in(Test) := { val testValue = test.in(Test).value; test.in(MultiJvm).value; testValue }
inConfig(MultiJvm)(SbtScalariform.configScalariformSettings)
inConfig(MultiJvm)(compileInputs.in(compile) := { scalariformFormat.value; compileInputs.in(compile).value })
AutomateHeaderPlugin.automateFor(Compile, Test, MultiJvm)
HeaderPlugin.settingsFor(Compile, Test, MultiJvm)
