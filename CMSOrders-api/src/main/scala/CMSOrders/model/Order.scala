/**
  * The orders API allows you to create, view, update, and delete individual, or a batch, of orders.
  * API for accessing ContosoCMS Order microservice
  *
  * OpenAPI spec version: 1.0.4
  * Contact: you@your-company.com
  *
  * NOTE: This class is auto generated by the swagger code generator program.
  * https://github.com/swagger-api/swagger-codegen.git
  * Do not edit the class manually.
  */

package CMSOrders.model

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
//import play.api.libs.json.JodaWrites._
//import play.api.libs.json.JodaReads._
case class Order(
                  registrantEmail: String,
                  registrantFirstName: Option[String],
                  registrantLastname: Option[String],
                  orderId: Option[UUID],
                  conferenceId: UUID,
                  createdDate: Option[DateTime],
                  attendees: Option[Seq[Attendee]],
                  status: Option[OrderStatusEnum.OrderStatusEnum]
                ) {
  //  def safeId = orderId.getOrElse(UUID.randomUUID())
}

object OrderStatusEnum extends Enumeration {
  val created, ready, submitted, reserved, partialReserved, canceled, paid, confirmed = Value
  type OrderStatusEnum = Value
  implicit val format: Format[Value] = Format(Reads.enumNameReads(this), Writes.enumNameWrites[OrderStatusEnum.type])
}

object Order {
  implicit val format: Format[Order] = Json.format
}