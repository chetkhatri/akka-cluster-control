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

import akka.cluster.{ Cluster, MemberStatus }
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.remote.testkit.{ MultiNodeConfig, MultiNodeSpec }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.testkit.TestDuration
import com.typesafe.config.ConfigFactory
import de.heikoseeberger.akkahttpcirce.CirceSupport
import de.heikoseeberger.akkasse.{ EventStreamUnmarshalling, ServerSentEvent }
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }
import scala.concurrent.duration.{ Duration, DurationInt }
import scala.concurrent.{ Await, Awaitable }

object ClusterControlConfig extends MultiNodeConfig {

  commonConfig(ConfigFactory.load()) // See /src/multi-jvm/resources/application.conf

  for (node <- 0 to 3) {
    val port = s"255$node"
    nodeConfig(role(port))(ConfigFactory.parseString(s"akka.remote.netty.tcp.port = $port"))
  }
}

class ClusterControlSpecMultiJvmNode0 extends ClusterControlSpec
class ClusterControlSpecMultiJvmNode1 extends ClusterControlSpec
class ClusterControlSpecMultiJvmNode2 extends ClusterControlSpec
class ClusterControlSpecMultiJvmNode3 extends ClusterControlSpec

abstract class ClusterControlSpec extends MultiNodeSpec(ClusterControlConfig)
    with WordSpecLike with Matchers with BeforeAndAfterAll with RequestBuilding {
  import CirceCodec._
  import CirceSupport._
  import ClusterControl._
  import EventStreamUnmarshalling._
  import io.circe.generic.auto._
  import system.dispatcher

  private val cluster = Cluster(system)

  private implicit val mat = ActorMaterializer()

  "A ClusterControl" should {
    "track the member nodes" in {

      var eventSource: Source[ServerSentEvent, Any] = null

      runOn(roles(1)) {
        result(
          Http(system).bindAndHandle(ClusterControl(cluster, 100), "127.0.0.1", cluster.selfAddress.port.get + 6000),
          10.seconds.dilated
        )
      }

      enterBarrier("HTTP server up")

      runOn(roles(1)) {
        cluster.join(cluster.selfAddress)
      }
      runOn(roles.head) {
        eventSource = result(getMemberNodeEvents())
        within(10.seconds.dilated) {
          awaitAssert {
            ports(result(getMemberNodes())) shouldBe Set(1).map(roles(_).name.toInt)
          }
        }
      }

      enterBarrier("First node up")

      runOn(roles(2)) {
        cluster.join(cluster.selfAddress.copy(port = Some(roles(1).name.toInt)))
      }
      runOn(roles.head) {
        within(10.seconds.dilated) {
          awaitAssert {
            ports(result(getMemberNodes())) shouldBe Set(1, 2).map(roles(_).name.toInt)
          }
        }
      }

      enterBarrier("Second node up")

      runOn(roles(3)) {
        cluster.join(cluster.selfAddress.copy(port = Some(roles(1).name.toInt)))
      }
      runOn(roles.head) {
        within(10.seconds.dilated) {
          awaitAssert {
            ports(result(getMemberNodes())) shouldBe Set(1, 2, 3).map(roles(_).name.toInt)
          }
        }
      }

      enterBarrier("Third node up")

      runOn(roles(3)) {
        // Wait until up before leaving, else leaving takes no effect at all!
        within(10.seconds.dilated) {
          awaitAssert {
            cluster.state.members.filter(_.status == MemberStatus.up).exists(_.address == cluster.selfAddress) shouldBe true
          }
        }
        cluster.leave(cluster.selfAddress)
      }
      runOn(roles.head) {
        within(10.seconds.dilated) {
          awaitAssert {
            ports(result(getMemberNodes())) shouldBe Set(1, 2).map(roles(_).name.toInt)
          }
        }
      }

      enterBarrier("Third node left")

      runOn(roles.head) {
        val events = result(
          eventSource.takeWhile(_ != sse(2553, "removed")).runFold(Vector.empty[ServerSentEvent])(_ :+ _),
          10.seconds.dilated
        )
        events should contain inOrder (
          // sse(2551, "joined"), there might be a race btw subscribing and reaching up
          sse(2551, "up"),
          sse(2552, "joined"),
          sse(2552, "up"),
          sse(2553, "joined"),
          sse(2553, "up"),
          sse(2553, "left"),
          sse(2553, "exited"),
          sse(2553, "unreachable")
        )
      }

      enterBarrier("Done")
    }
  }

  override def initialParticipants = roles.size

  override protected def beforeAll() = {
    super.beforeAll()
    multiNodeSpecBeforeAll()
  }

  override protected def afterAll() = {
    multiNodeSpecAfterAll()
    super.afterAll()
  }

  private def result[A](awaitable: Awaitable[A], duration: Duration = 3.seconds.dilated) =
    Await.result(awaitable, duration)

  private def getMemberNodes() = Http(system)
    .singleRequest(Get("http://127.0.0.1:8551/cluster-control/member-nodes"))
    .flatMap(Unmarshal(_).to[Set[ClusterControl.MemberNode]])

  private def getMemberNodeEvents() = Http(system)
    .singleRequest(Get("http://127.0.0.1:8551/cluster-control/member-node-events"))
    .flatMap(Unmarshal(_).to[Source[ServerSentEvent, Any]])

  private def ports(memberNodes: Set[MemberNode]) =
    memberNodes.withFilter(_.status == MemberStatus.up).flatMap(_.address.port)

  private def sse(port: Int, kind: String) = ServerSentEvent(cluster.selfAddress.copy(port = Some(port)).toString, kind)
}
