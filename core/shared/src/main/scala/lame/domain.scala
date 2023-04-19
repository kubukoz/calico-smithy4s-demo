package lame

import io.circe.Codec

import cats.kernel.Eq
import cats.kernel.Monoid

case class Payload(data: Map[String, List[Coordinates]]) derives Codec.AsObject {
  def -(clientId: String): Payload = copy(data = data - clientId)
  def withNewClient(clientId: String): Payload = copy(data = data + (clientId -> Nil))

  def +(clientId: String, coords: List[Coordinates]): Payload = copy(data =
    data + (clientId -> coords)
  )

  def ++(other: Payload): Payload = copy(data = data ++ other.data)

}

object Payload {

  given Eq[Payload] = Eq.fromUniversalEquals
  given Monoid[Payload] = Monoid.instance(Payload(Map.empty), _ ++ _)
}

case class Coordinates(x: Int, y: Int) derives Codec.AsObject

object Coordinates {

  given Eq[Coordinates] = Eq.fromUniversalEquals
}
