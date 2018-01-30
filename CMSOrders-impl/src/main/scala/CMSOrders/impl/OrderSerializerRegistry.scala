package CMSOrders.impl

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import org.joda.time.DateTime
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._


object OrderSerializerRegistry extends JsonSerializerRegistry {
  override def serializers = List(
    // start of events
    JsonSerializer[OrderPlaced],
    JsonSerializer[AttendeesAdded],
    // end of events
    // start of commands
    JsonSerializer[CreateOrderCMD],
    JsonSerializer[AddAttendeesCMD],
    JsonSerializer[ReadOrderCommand.type],


    // end of commands
    // start of model
    JsonSerializer[OrderReq]
    // end of model


  )
}
