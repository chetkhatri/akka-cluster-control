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
import cats.data.Xor
import io.circe.{ Decoder, DecodingFailure, Encoder, HCursor, Json }

object CirceCodec {

  implicit val addressEncoder = new Encoder[Address] {
    override def apply(address: Address) = Json.string(address.toString)
  }

  implicit val addressDecoder = new Decoder[Address] {
    override def apply(cursor: HCursor) = cursor.focus.as[String].flatMap { s =>
      Xor.catchNonFatal(AddressFromURIString(s)).leftMap(t => DecodingFailure(t.getMessage, cursor.history))
    }
  }
}
