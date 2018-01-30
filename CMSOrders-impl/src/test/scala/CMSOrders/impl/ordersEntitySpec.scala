package CMSOrders.impl

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.datastax.driver.core.utils.UUIDs
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, WordSpec}
//import play.api.libs.json.Json
//import com.github.nscala_money.money.Imports._

class ordersEntitySpec extends WordSpec with Matchers with BeforeAndAfterAll with OptionValues {

  private val system = ActorSystem("test", JsonSerializerRegistry.actorSystemSetupFor(OrderSerializerRegistry))

  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }

  val orderId1 = UUIDs.timeBased()
  val orderId2 = UUIDs.timeBased()
  val conferenceId1 = UUIDs.timeBased()
  val conferenceId2 = UUIDs.timeBased()
  val orderRecievedDateTime = DateTime.now
  val attendee1 = CMSOrders.model.Attendee("abc@abc.com", Some("FirstName"), Some("Last Name"), Some(100))
  val addtionalAttendee = CMSOrders.impl.Attendee("new@new.com", Some("NewFirstName"), Some("NewLast Name"), Some(100))
  val attendee2 = CMSOrders.model.Attendee("axy@xyz.com", None, None, Some(100))
  val attendees = Seq(attendee1, attendee2)
  val order1 = CMSOrders.model.Order("John1@John.com", Some("John1"), Some("Mesgner1"),
    Some(orderId1), conferenceId1, Some(DateTime.now), Some(attendees), Some(CMSOrders.model.OrderStatusEnum.ready))
  val order2 = CMSOrders.model.Order("John2@John.com", Some("John2"), Some("Mesgner2"),
    Some(orderId2), conferenceId2, Some(DateTime.now), Some(attendees), Some(CMSOrders.model.OrderStatusEnum.created))
  val allOrders = Seq(order1, order2)

  val orderReq1 = OrderReq(order1.registrantEmail, order1.registrantFirstName,
    order1.registrantLastname, orderId1, conferenceId1,
    orderRecievedDateTime, None, OrderStatusEnum.created)

  val orderReq2 = OrderReq(order2.registrantEmail, order2.registrantFirstName,
    order2.registrantLastname, orderId2, conferenceId2,
    orderRecievedDateTime, None, OrderStatusEnum.created)

  private def withDriver[T](block: PersistentEntityTestDriver[OrderCommand, OrderEvent, Option[OrderReq]] => T): T = {
    val driver = new PersistentEntityTestDriver(system, new OrderEntity, orderId1.toString)
    try {
      block(driver)
    } finally {
      driver.getAllIssues shouldBe empty
    }
  }

  "The Order  Entity " should {

    "allow creating a order request" in withDriver { driver =>
      val outcome = driver.run(CreateOrderCMD(orderReq1))
      outcome.events should contain only OrderPlaced(orderReq1)
      outcome.state should ===(Some(orderReq1))


      //      val outcome2 = driver.run(ReadOrderCommand)

      //      val jsonString = Json.toJson(outcome2.state)
      //      val prettyJson = Json.prettyPrint(jsonString)
      //      println(prettyJson)
    }

    "allow retrieve an order" in withDriver { driver =>
      val outcome1 = driver.run(CreateOrderCMD(orderReq1))
      outcome1.events should contain only OrderPlaced(orderReq1)
      outcome1.state should ===(Some(orderReq1))

      val outcome2 = driver.run(ReadOrderCommand)
      outcome2.replies should contain only outcome1.state
      //      val jsonString = Json.toJson(outcome2.state)
      //      val prettyJson = Json.prettyPrint(jsonString)
      //      println(prettyJson)
    }

    "allow add attendees to  an order" in withDriver { driver =>
      val outcome1 = driver.run(CreateOrderCMD(orderReq1))
      //      outcome1.events should contain only OrderPlaced(orderReq1)
      //      outcome1.state should ===(Some(orderReq1))

      val outcome2 = driver.run(ReadOrderCommand)
      //      outcome2.replies should contain only outcome1.state

      val outcome3 = driver.run(AddAttendeesCMD(addtionalAttendee))
      outcome3.events should contain only AttendeesAdded(addtionalAttendee)
    }

  }
}
