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

import akka.actor.{ Address, AddressFromURIString }
import akka.cluster.ClusterEvent.{ ClusterDomainEvent, InitialStateAsEvents, MemberEvent, MemberExited, MemberJoined, MemberLeft, MemberRemoved, MemberUp, ReachabilityEvent, ReachableMember, UnreachableMember }
import akka.cluster.{ Cluster, Member, MemberStatus }
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ Directive0, Directives, Route }
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import de.heikoseeberger.akkahttpcirce.CirceSupport
import de.heikoseeberger.akkasse.{ EventStreamMarshalling, ServerSentEvent }
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{ DurationInt, FiniteDuration }

object ClusterControl {

  object MemberNode {
    def fromMember(member: Member): MemberNode = MemberNode(EncAddress(member.address), member.address, member.status)
  }
  case class MemberNode(encAddress: EncAddress, address: Address, status: MemberStatus)

  type EncAddress = String
  object EncAddress {
    def apply(address: Address): EncAddress = Base64.getUrlEncoder.encodeToString(address.toString.getBytes(UTF_8))
    def unapply(encAddress: EncAddress): Option[Address] =
      AddressFromURIString.unapply(new String(Base64.getUrlDecoder.decode(encAddress), UTF_8))
  }

  def apply(
    cluster: Cluster,
    eventBufferSize: Int = 1024,
    endpoint: Directive0 = Directives.pathPrefix("cluster-control"),
    keepAliveInterval: FiniteDuration = 30.seconds
  )(
    implicit
    ec: ExecutionContext
  ): Route = {
    import CirceCodec._
    import CirceSupport._
    import Directives._
    import EventStreamMarshalling._
    import io.circe.generic.auto._

    // format: OFF
    endpoint {
      pathPrefix("member-nodes") {
        path(Segment) {
          case EncAddress(address) =>
            delete {
              complete {
                if (cluster.state.members.exists(_.address == address)) {
                  if (cluster.state.unreachable.exists(_.address == address))
                    cluster.down(address)
                  else
                    cluster.leave(address)
                  StatusCodes.NoContent
                } else
                  StatusCodes.NotFound -> s"$address is not a member node!"
              }
            }
          case unknown =>
            complete {
              StatusCodes.BadRequest -> s"$unknown can't be decoded as a valid address!"
            }
        } ~
        get {
          complete {
            cluster.state.members.map(MemberNode.fromMember)
          }
        }
      } ~
      path("member-node-events") {
        get {
          complete {
            Source.actorRef[ClusterDomainEvent](eventBufferSize, OverflowStrategy.dropHead)
              .map(toServerSentEvent)
              .mapMaterializedValue(cluster.subscribe(_, InitialStateAsEvents, classOf[MemberEvent], classOf[ReachabilityEvent]))
              .keepAlive(keepAliveInterval, () => ServerSentEvent.heartbeat)
          }
        }
      }
    }
    // format: ON
  }

  private def toServerSentEvent(event: ClusterDomainEvent) = {
    def sse(member: Member, eventType: String) = {
      import io.circe.generic.auto._
      import io.circe.syntax._
      ServerSentEvent(MemberNode.fromMember(member).asJson.noSpaces, eventType)
    }
    event match {
      case MemberJoined(member)      => sse(member, "joined")
      case MemberUp(member)          => sse(member, "up")
      case MemberLeft(member)        => sse(member, "left")
      case MemberExited(member)      => sse(member, "exited")
      case MemberRemoved(member, _)  => sse(member, "removed")
      case ReachableMember(member)   => sse(member, "reachable")
      case UnreachableMember(member) => sse(member, "unreachable")
      case _                         => throw new IllegalStateException("Impossible, because only subscribed to MemberEvents and ReachabilityEvents!")
    }
  }
}
