package CMSOrders.impl

import java.util.UUID

import CMSOrders.util.JsonFormats._
import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import org.joda.time.DateTime
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{Format, Json, Reads, Writes}

//import play.api.libs.json.JodaWrites._
//import play.api.libs.json.JodaReads._

import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._




class OrderEntity extends PersistentEntity {
  private final val log: Logger =
    LoggerFactory.getLogger(classOf[OrderEntity])
  override type Command = OrderCommand
  override type Event = OrderEvent
  override type State = Option[OrderReq]

  override def initialState: Option[OrderReq] = None

  def processCreatedOrders(orderReq: OrderReq): OrderEntity.this.Actions = {
    log.debug(" orderReqId " + orderReq.orderId.toString + "Status " + orderReq.status )
    Actions().onCommand[AddAttendeesCMD, Done] {
      case (AddAttendeesCMD(attendee), ctx, state) =>
        ctx.thenPersist(AttendeesAdded(attendee))(_ => ctx.reply(Done))
    }.onCommand[UpdateRegistrantCMD, Done] {
      case (UpdateRegistrantCMD(registrantReq), ctx, state) =>
        ctx.thenPersist(RegistrantUpdated(registrantReq))(_ => ctx.reply(Done))
    }.onEvent {
      case (AttendeesAdded(attendee), _) => Some(orderReq.addAttendees(Seq(attendee)))
    }.onEvent {
      case (RegistrantUpdated(registrantReq), _) => Some(orderReq.updateRegistrantInfo(registrantReq))
    }.orElse(processReadOrderCommand)
  }

  override def behavior = {

    case None => orderNotCreated
    case Some(order) if order.status == OrderStatusEnum.created => processCreatedOrders(order)
    case Some(order) if order.status != OrderStatusEnum.created => processCreatedOrders(order)

  }

  private val processReadOrderCommand = Actions().onReadOnlyCommand[ReadOrderCommand.type, Option[OrderReq]] {
    case (ReadOrderCommand, ctx, state) => ctx.reply(state)
  }
  private val orderNotCreated = {
    Actions().onCommand[CreateOrderCMD, Done] {
      case (CreateOrderCMD(createOrderCMD), ctx, state) =>
        ctx.thenPersist(OrderPlaced(createOrderCMD))(_ => ctx.reply(Done))
    }.onEvent {
      case (OrderPlaced(orderReq), state) => Some(orderReq)
    }.orElse(processReadOrderCommand)
  }


}

// start of state
case class OrderReq(
                     registrantEmail: String,
                     registrantFirstName: Option[String],
                     registrantLastname: Option[String],
                     orderId: UUID,
                     conferenceId: UUID,
                     createdDate: DateTime,
                     attendees: Option[Seq[Attendee]],
                     status: OrderStatusEnum.OrderStatusEnum
                   ) {
  def updateStatus(newStatus: OrderStatusEnum.Value) = {
    copy(
      status = newStatus
    )
  }


  def addAttendees(newAttendees: Seq[Attendee]) = {
    assert(status == OrderStatusEnum.created)
    val updatedAttendees = attendees.toSeq.flatten ++ newAttendees
    copy(
      attendees = Some(updatedAttendees.distinct)
    )
  }

  def updateRegistrantInfo(registrantReq: RegistrantReq) = {
    assert(status == OrderStatusEnum.created)
    copy(
      registrantFirstName = registrantReq.registrantFirstName,
      registrantLastname = registrantReq.registrantLastname
    )
  }
}

object OrderStatusEnum extends Enumeration {
  val created, ready, submitted, reserved, partialReserved, canceled, paid, confirmed, deleted = Value
  type OrderStatusEnum = Value
  implicit val format: Format[Value] = Format(Reads.enumNameReads(this), Writes.enumNameWrites[OrderStatusEnum.type])
}

object OrderReq {
  implicit val format: Format[OrderReq] = Json.format
}


case class Attendee(
                     email: String,
                     firstName: Option[String],
                     lastName: Option[String],
                     seatNumber: Option[Int]
                   )

object Attendee {
  implicit val format: Format[Attendee] = Json.format
}

case class RegistrantReq(registrantEmail: String,
                         registrantSecondaryEmail: String,
                         registrantFirstName: Option[String],
                         registrantLastname: Option[String])

object RegistrantReq {
  implicit val format: Format[RegistrantReq] = Json.format
}

// end of state

// star of commands
sealed trait OrderCommand

case class CreateOrderCMD(createOrderREQ: OrderReq) extends OrderCommand with ReplyType[Done]

object CreateOrderCMD {
  implicit val format: Format[CreateOrderCMD] = Json.format
}


case class UpdateRegistrantCMD(registrantReq: RegistrantReq) extends OrderCommand with ReplyType[Done]

object UpdateRegistrantCMD {
  implicit val format: Format[UpdateRegistrantCMD] = Json.format
}


case class AddAttendeesCMD(attendee: Attendee) extends OrderCommand with ReplyType[Done]

object AddAttendeesCMD {
  implicit val format: Format[AddAttendeesCMD] = Json.format
}



/**
  * Read one order for a give orderId
  */
case object ReadOrderCommand extends OrderCommand with ReplyType[Option[OrderReq]] {
  implicit val format: Format[ReadOrderCommand.type] = singletonFormat(ReadOrderCommand)
}

// end of commands

// start of events
object OrderEvent {
  val NumShards = 4
  val Tag = AggregateEventTag.sharded[OrderEvent](NumShards)
}

sealed trait OrderEvent extends AggregateEvent[OrderEvent] {
  override def aggregateTag = OrderEvent.Tag
}

case class OrderPlaced(orderReq: OrderReq) extends OrderEvent

object OrderPlaced {
  implicit val format: Format[OrderPlaced] = Json.format
}

case class AttendeesAdded(attendee: Attendee) extends OrderEvent

object AttendeesAdded {
  implicit val format: Format[AttendeesAdded] = Json.format
}

case class RegistrantUpdated(registrantReq: RegistrantReq) extends OrderEvent

object RegistrantUpdated {
  implicit val format: Format[RegistrantUpdated] = Json.format
}




// end of events

