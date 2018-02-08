package CMSOrders.impl

import java.util.UUID

import CMSOrders.api.RegistrantApi
import CMSOrders.impl.OrderStatusEnum.OrderStatusEnum
import CMSOrders.model.{Order, Registrant}
import CMSOrders.util.InvalidIdentifierException
import akka.{Done, NotUsed}
import com.datastax.driver.core.utils.UUIDs
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{NotFound, ResponseHeader}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import org.joda.time.DateTime

import scala.util.Try
//import play.api.libs.json.JodaWrites._
//import play.api.libs.json.JodaReads._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class OrderServiceImpl(registry: PersistentEntityRegistry)
                      (implicit ec: ExecutionContext) extends RegistrantApi {
  private final val log: Logger =
    LoggerFactory.getLogger(classOf[OrderServiceImpl])

  /**
    * Add or update an attendee
    * Conference id are mandatory for creating an order.
    *
    * @param conferenceId conferenceId
    * @param orderId      orderId
    * @return void Body Parameter  Order object
    */
  override def addAttendees(conferenceId: String, orderId: String): ServiceCall[CMSOrders.model.Attendee, String] =
    ServerServiceCall { (requestHeader, addtionalAttendee) => {
      val conferenceUUID = Try({
        UUID.fromString(conferenceId)
      }).getOrElse(throw InvalidIdentifierException("Invalid Conference ID : "+conferenceId))
      val orderUUID = Try({
        UUID.fromString(orderId)
      }).getOrElse(throw InvalidIdentifierException("Invalid orderId ID : "+orderId))

      val addtionalAttendeeImpl = CMSOrders.impl.Attendee(addtionalAttendee.email, addtionalAttendee.firstName, addtionalAttendee.lastName, addtionalAttendee.seatNumber)
      for {
        _ <- entityRef(orderUUID).ask(AddAttendeesCMD(addtionalAttendeeImpl))
      } yield {
        val responseHeader = ResponseHeader.Ok.withStatus(201)
          .withHeader("Location", orderId.toString)
        log.debug("Added attendee to order : {} ", orderId.toString)
        (responseHeader, orderId.toString)
      }

    }
  }

  /**
    * Create a new order
    * Conference id are mandatory for creating an order.
    *
    * @param conferenceId conferenceId
    * @return void Body Parameter  Order object
    */
  override def createOrder(conferenceId: String): ServiceCall[Order, String] = ServerServiceCall { (requestHeader, recievedOrder) => {
    val orderRecievedDateTime = DateTime.now
    val conferenceUUID = Try({
      UUID.fromString(conferenceId)
    }).getOrElse(throw InvalidIdentifierException("Invalid Conference ID"))
    val orderId = UUIDs.timeBased()
    val orderReq = OrderReq(recievedOrder.registrantEmail, recievedOrder.registrantFirstName,
      recievedOrder.registrantLastname, orderId, conferenceUUID,
      orderRecievedDateTime, None, OrderStatusEnum.created)
    log.debug("Order creted with orderID :  {}", orderId.toString)
    log.debug("Order creted with ConferenceID  :  {}", recievedOrder.conferenceId)
    for {
      _ <- entityRef(orderReq.orderId).ask(CreateOrderCMD(orderReq))
    } yield {
      val responseHeader = ResponseHeader.Ok.withStatus(201)
        .withHeader("Location", orderId.toString)
      log.debug("createdOrderId.toString : {} ", orderId.toString)
      (responseHeader, orderId.toString)
    }

  }
  }

  /**
    * Delete a order
    * Deletes  existing orders for a given  conference id and order id.
    *
    * @param conferenceId Conference id
    * @param orderId      Unique orderId
    * @return void
    */
  override def deleteOrder(conferenceId: String, orderId: String) = ???

  /**
    * Get all Orders
    * Allows you to retrieve the list of existing orders.
    *
    * @param page    Page number (optional)
    * @param perPage page size , rows per page (optional)
    * @return Seq[Order]
    */
  override def getOrders(page: Option[Int], perPage: Option[Int]) = ServerServiceCall { _ => {

    val orderId1 = UUIDs.timeBased()
    val orderId2 = UUIDs.timeBased()
    val conferenceId1 = UUIDs.timeBased()
    val conferenceId2 = UUIDs.timeBased()
    val attendee1 = CMSOrders.model.Attendee("abc@abc.com", Some(page.toString), Some(perPage.toString), perPage)
    val attendee2 = CMSOrders.model.Attendee("axy@xyz.com", None, None, perPage)
    val attendees = Seq(attendee1, attendee2)
    val order1 = CMSOrders.model.Order("John1@John.com", Some("John1"), Some("Mesgner1"),
      Some(orderId1), conferenceId1, Some(DateTime.now), Some(attendees), Some(CMSOrders.model.OrderStatusEnum.ready))
    val order2 = CMSOrders.model.Order("John2@John.com", Some("John2"), Some("Mesgner2"),
      Some(orderId2), conferenceId2, Some(DateTime.now), Some(attendees), Some(CMSOrders.model.OrderStatusEnum.created))
    val allOrders = Seq(order1, order2)
    Future.successful(allOrders)
  }
  }

  /**
    * Get all Orders of a conference
    * Allows you to retrieve the list of existing orders.
    *
    * @param conferenceId Conference id
    * @param page         Page number (optional)
    * @param perPage      page size , rows per page (optional)
    * @return Seq[Order]
    */
  override def getOrdersbyConferenceId(page: Option[Int], perPage: Option[Int], conferenceId: String) = ???

  private[impl] def convertOrder(orderReq: OrderReq): CMSOrders.model.Order = {
    CMSOrders.model.Order(
      orderReq.registrantEmail,
      orderReq.registrantFirstName,
      orderReq.registrantLastname,
      Some(orderReq.orderId),
      orderReq.conferenceId,
      Some(orderReq.createdDate),
      convertAttendee(orderReq.attendees),
      convertOrderStatus(Some(orderReq.status)))
  }

  private[impl] def convertOrderStatus(orderReqStatus: Option[OrderStatusEnum]): Option[CMSOrders.model.OrderStatusEnum.OrderStatusEnum] = {
    orderReqStatus match {
      case OrderStatusEnum.canceled => Some(CMSOrders.model.OrderStatusEnum.canceled)
      case OrderStatusEnum.confirmed => Some(CMSOrders.model.OrderStatusEnum.confirmed)
      case OrderStatusEnum.created => Some(CMSOrders.model.OrderStatusEnum.created)
      case OrderStatusEnum.paid => Some(CMSOrders.model.OrderStatusEnum.paid)
      case OrderStatusEnum.partialReserved => Some(CMSOrders.model.OrderStatusEnum.partialReserved)
      case OrderStatusEnum.ready => Some(CMSOrders.model.OrderStatusEnum.ready)
      case OrderStatusEnum.reserved => Some(CMSOrders.model.OrderStatusEnum.reserved)
      case OrderStatusEnum.submitted => Some(CMSOrders.model.OrderStatusEnum.submitted)
      case OrderStatusEnum.deleted => Some(CMSOrders.model.OrderStatusEnum.deleted)
      case _ => Some(CMSOrders.model.OrderStatusEnum.created)
    }
  }

  private[impl] def convertAttendee(attendees: Option[Seq[Attendee]]): Option[Seq[CMSOrders.model.Attendee]] = {
    attendees match {
      case Some(persons) => {
        Some(for {
          attendee <- persons
        } yield
          CMSOrders.model.Attendee(attendee.email, attendee.firstName, attendee.lastName, attendee.seatNumber))
      }
      case None => None
    }
    //    CMSOrders.model.Attendee(attendee.email,attendee.firstName,attendee.lastName,attendee.seatNumber)
  }

  /**
    * Get  an Order
    * Allows you to retrieve the list of existing orders by conference id and order id.
    *
    * @param conferenceId Conference id
    * @param orderId      Unique orderId
    * @param page         Page number (optional)
    * @param perPage      page size , rows per page (optional)
    * @return Order
    */
  override def getOrdersbyOrderId(conferenceId: String, orderId: String, page: Option[Int] = None, perPage: Option[Int] = None) = ServerServiceCall { _ =>

    val orderIdUUId = Try({
      UUID.fromString(orderId)
    }).getOrElse(throw InvalidIdentifierException(s"Invalid Conference ID : ${orderId}"))

    entityRef(orderIdUUId)
      .ask(ReadOrderCommand)
      .map {
        case Some(order) => convertOrder(order)
        case None => throw NotFound(s"Order with :  ${orderId}   not found")
      }
  }

  /**
    * Remove attendee
    *
    * @param conferenceId conferenceId
    * @param orderId      orderId
    * @param seatNumber   seatNumber
    * @return void
    */
  override def ordersConferenceIdOrderIdSeatNumberDelete(conferenceId: String, orderId: String, seatNumber: String) = ???

  /**
    * Modify a an order
    * Conference id are mandatory for creating an order.
    *
    * @param conferenceId conferenceId
    * @return void Body Parameter  Order object
    */
  override def updateRegistrantInfo(conferenceId: String,orderId: String) : ServiceCall[Registrant, String] = ServerServiceCall { (requestHeader, registrantInfo) => {
    val orderRecievedDateTime = DateTime.now
    val conferenceUUID = Try({
      UUID.fromString(conferenceId)
    }).getOrElse(throw InvalidIdentifierException(s"Invalid Conference ID"))
    val orderIdUUID = Try({
      UUID.fromString(orderId)
    }).getOrElse(throw InvalidIdentifierException(s"Invalid Order ID"))
    val registrantReq = RegistrantReq(registrantInfo.registrantEmail,registrantInfo.registrantSecondaryEmail,registrantInfo.registrantFirstName,registrantInfo.registrantLastname)
    log.debug(s"Order created with orderID :  ${orderId.toString}")
    for {
      _ <- entityRef(orderIdUUID).ask(UpdateRegistrantCMD(registrantReq))
    } yield {
      val responseHeader = ResponseHeader.Ok.withStatus(201)
        .withHeader("Location", orderId.toString)
      log.debug(s"Updated the createdOrderId.toString : ${orderId.toString}")
      (responseHeader, orderId.toString)
    }

  }
  }


  private def entityRef(orderId: UUID) = entityRefString(orderId.toString)

  private def entityRefString(orderId: String) = registry.refFor[OrderEntity](orderId)
}
