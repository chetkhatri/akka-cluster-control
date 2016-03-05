/*
 * Copyright 2016 Heiko Seeberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.heikoseeberger.akkaclustercontrol
package app

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{ Failure, Success }

object ClusterControlApp {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("cluster-control-system")
    implicit val mat = ActorMaterializer()
    import system.dispatcher

    val log = Logging(system, this.getClass)
    val address = system.settings.config.getString("akka-cluster-control.http.address")
    val port = system.settings.config.getInt("akka-cluster-control.http.port")

    def route = {
      import Directives._
      // format: OFF
      ClusterControl(Cluster(system), endpoint = Directives.rawPathPrefix(Directives.Neutral)) ~
      getFromResourceDirectory("web") ~
      redirect("index.html", StatusCodes.PermanentRedirect)
      // format: ON
    }

    Http(system)
      .bindAndHandle(route, address, port)
      .onComplete {
        case Success(Http.ServerBinding(socketAddress)) =>
          log.info("Listening on {}", socketAddress)
          Await.ready(system.whenTerminated, Duration.Inf)
        case Failure(cause) =>
          log.error(cause, "Can't bind to {}:{}", address, port)
          Await.ready(system.terminate(), Duration.Inf)
      }
  }
}
