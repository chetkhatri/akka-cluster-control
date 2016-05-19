import sbt._

object Version {
  final val Akka         = "2.4.6"
  final val AkkaHttpJson = "1.6.0"
  final val AkkaLog4j    = "1.1.3"
  final val AkkaSse      = "1.8.0"
  final val Circe        = "0.4.1"
  final val Constructr   = "0.13.2"
  final val Log4j        = "2.5"
  final val Scala        = "2.11.8"
  final val ScalaTest    = "2.2.6"
}

object Library {
  val akkaCluster                = "com.typesafe.akka"        %% "akka-cluster"                 % Version.Akka
  val akkaHttp                   = "com.typesafe.akka"        %% "akka-http-experimental"       % Version.Akka
  val akkaHttpCirce              = "de.heikoseeberger"        %% "akka-http-circe"              % Version.AkkaHttpJson
  val akkaHttpTestkit            = "com.typesafe.akka"        %% "akka-http-testkit"            % Version.Akka
  val akkaLog4j                  = "de.heikoseeberger"        %% "akka-log4j"                   % Version.AkkaLog4j
  val akkaMultiNodeTestkit       = "com.typesafe.akka"        %% "akka-multi-node-testkit"      % Version.Akka
  val akkaSse                    = "de.heikoseeberger"        %% "akka-sse"                     % Version.AkkaSse
  val akkaTestkit                = "com.typesafe.akka"        %% "akka-testkit"                 % Version.Akka
  val circeGeneric               = "io.circe"                 %% "circe-generic"                % Version.Circe
  val constructrAkka             = "de.heikoseeberger"        %% "constructr-akka"              % Version.Constructr
  val constructrCoordinationEtcd = "de.heikoseeberger"        %% "constructr-coordination-etcd" % Version.Constructr
  val log4jCore                  = "org.apache.logging.log4j" %  "log4j-core"                   % Version.Log4j
  val scalaTest                  = "org.scalatest"            %% "scalatest"                    % Version.ScalaTest
}
