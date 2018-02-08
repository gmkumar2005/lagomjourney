package CMSOrders.impl

import CMSOrders.api.RegistrantApi
import com.datastax.driver.core.utils.UUIDs
import com.lightbend.lagom.scaladsl.api.AdditionalConfiguration
import com.lightbend.lagom.scaladsl.api.transport.ResponseHeader
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LocalServiceLocator}
import com.lightbend.lagom.scaladsl.testkit.{ServiceTest, TestTopicComponents}
import org.joda.time.DateTime
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import play.api.Configuration
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{Future, Promise}
//import play.api.libs.json.Json
import play.api.libs.json.Json

/**
  *
  */
class OrderServiceImplIntegrationTest extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  private val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra(true)) { ctx =>
    new LagomApplication(ctx) with OrderComponents with LocalServiceLocator with AhcWSComponents with TestTopicComponents {
      override def additionalConfiguration: AdditionalConfiguration =
        super.additionalConfiguration ++ Configuration.from(Map(
          "cassandra-query-journal.eventual-consistency-delay" -> "0"
        ))
    }
  }

  val itemService = server.serviceClient.implement[RegistrantApi]
  val orderService = server.serviceClient.implement[RegistrantApi]

  val orderId1 = UUIDs.timeBased()
  val orderId2 = UUIDs.timeBased()
  val conferenceId1 = UUIDs.timeBased()
  val conferenceId2 = UUIDs.timeBased()
  val orderRecievedDateTime = DateTime.now
  val attendee1 = CMSOrders.model.Attendee("abc@abc.com", Some("FirstName"), Some("Last Name"), Some(100))
  val attendee2 = CMSOrders.model.Attendee("axy@xyz.com", None, None, Some(100))
  val addtionalAttendee = CMSOrders.model.Attendee("addtionalaxy@xyz.com", Some("Addtional One Name"), Some("Addtional One Lastname"), Some(100))
  val addtionalAttendee2 = CMSOrders.model.Attendee("2addtionalaxy@xyz.com", Some("2Addtional Name"), Some("2Addtional Lastname"), Some(2100))
  val attendees = Seq(attendee1, attendee2)
  val order1 = CMSOrders.model.Order("John1@John.com", Some("John1"), Some("Mesgner1"),
    Some(orderId1), conferenceId1, Some(DateTime.now), Some(attendees), Some(CMSOrders.model.OrderStatusEnum.ready))
  val order2 = CMSOrders.model.Order("John2@John.com", Some("John2"), Some("Mesgner2"),
    Some(orderId2), conferenceId2, Some(DateTime.now), Some(attendees), Some(CMSOrders.model.OrderStatusEnum.created))
  val allOrders = Seq(order1, order2)

  override def afterAll = server.stop()

  val orderReq1 = OrderReq(order1.registrantEmail, order1.registrantFirstName,
    order1.registrantLastname, orderId1, conferenceId1,
    orderRecievedDateTime, None, OrderStatusEnum.created)

  val orderReq2 = OrderReq(order2.registrantEmail, order2.registrantFirstName,
    order2.registrantLastname, orderId2, conferenceId2,
    orderRecievedDateTime, None, OrderStatusEnum.created)
  "The Order  service" should {

    "allow creating an order " in {

      for {
        created <- createOrderReq(order1)
        retrieved <- retrieveOrder(created._2)
      } yield {
//        val jsonString = Json.toJson(retrieved)
//        val prettyJson = Json.prettyPrint(jsonString)
//        println("retrieved Order : ")
//        println(prettyJson)

        created._2 should ===(retrieved.orderId.getOrElse("UnKnownOrderID").toString)

        OrderStatusEnum.created.toString should ===(retrieved.status.getOrElse("UnKnownStatus").toString)

        val expectedResponseHeader = ResponseHeader.Ok.withStatus(201)
          .withHeader("Location", retrieved.orderId.getOrElse("UnKnownOrderID").toString)

        expectedResponseHeader.status should ===(created._1.status)

        created._1.getHeader("Location") should ===(expectedResponseHeader.getHeader("Location"))
      }
    }

    "allow add attendee an order " in {

      for {
        created <- createOrderReq(order1)
        retrieved <- retrieveOrder(created._2)
        _ <- addAttendees(addtionalAttendee, retrieved)
        _ <- addAttendees(addtionalAttendee2, retrieved)
        afterAddAttendee <- retrieveOrder(created._2)
      } yield {
        //        val jsonString = Json.toJson(afterAddAttendee)
        //        val prettyJson = Json.prettyPrint(jsonString)
        //        println("retrieved Order afterAddAttendee : ")
        //        println(prettyJson)
        created._2 should ===(retrieved.orderId.getOrElse("UnKnownOrderID").toString)
        OrderStatusEnum.created.toString should ===(retrieved.status.getOrElse("UnKnownStatus").toString)

        val expectedResponseHeader = ResponseHeader.Ok.withStatus(201)
          .withHeader("Location", retrieved.orderId.getOrElse("UnKnownOrderID").toString)

        expectedResponseHeader.status should ===(created._1.status)
        created._1.getHeader("Location") should ===(expectedResponseHeader.getHeader("Location"))
        //        afterAddAttendee.attendees.getOrElse("EMPTYLIST") should === (Seq(addtionalAttendee,addtionalAttendee2))
        afterAddAttendee.attendees.getOrElse(Seq("EMPTYLIST")) should contain(addtionalAttendee)
        afterAddAttendee.attendees.getOrElse(Seq("EMPTYLIST")) should contain(addtionalAttendee2)
      }
    }


    }


  private def createOrderReq(order: CMSOrders.model.Order) = {
    orderService.createOrder(conferenceId1.toString).withResponseHeader.invoke(order)
  }

  private def addAttendees(attendee: CMSOrders.model.Attendee, order: CMSOrders.model.Order) = {
    orderService.addAttendees(order.conferenceId.toString, order.orderId.getOrElse("UNKNOWNORDERID").toString).invoke(attendee)
  }

  private def retrieveOrder(orderId: String) = {
    orderService.getOrdersbyOrderId(orderId, orderId, None, None).invoke
  }

  def awaitSuccess[T](maxDuration: FiniteDuration = 10.seconds, checkEvery: FiniteDuration = 100.milliseconds)(block: => Future[T]): Future[T] = {
    val checkUntil = System.currentTimeMillis() + maxDuration.toMillis

    def doCheck(): Future[T] = {
      block.recoverWith {
        case recheck if checkUntil > System.currentTimeMillis() =>
          val timeout = Promise[T]()
          server.application.actorSystem.scheduler.scheduleOnce(checkEvery) {
            timeout.completeWith(doCheck())
          }(server.executionContext)
          timeout.future
      }
    }

    doCheck()
  }

}
