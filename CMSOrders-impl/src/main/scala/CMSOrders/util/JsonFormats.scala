package CMSOrders.util

import java.time.Duration
import java.util.UUID

import play.api.libs.json.{JsonValidationError, _}

import scala.util.Try


object JsonFormats {


  def enumWrites[E <: Enumeration]: Writes[E#Value] = Writes(v => JsString(v.toString))


  def singletonReads[O](singleton: O): Reads[O] = {
    (__ \ "value").read[String].collect(
      JsonValidationError(s"Expected a JSON object with a single field with key 'value' and value '${singleton.getClass.getSimpleName}'")
    ) {
      case s if s == singleton.getClass.getSimpleName => singleton
    }
  }

  def singletonWrites[O]: Writes[O] = Writes { singleton =>
    Json.obj("value" -> singleton.getClass.getSimpleName)
  }

  def singletonFormat[O](singleton: O): Format[O] = {
    Format(singletonReads(singleton), singletonWrites)
  }

  implicit val uuidReads: Reads[UUID] = implicitly[Reads[String]]
    .collect(JsonValidationError("Invalid UUID"))(Function.unlift { str =>
      Try(UUID.fromString(str)).toOption
    })
  implicit val uuidWrites: Writes[UUID] = Writes { uuid =>
    JsString(uuid.toString)
  }

  implicit val durationReads: Reads[Duration] = implicitly[Reads[String]]
    .collect(JsonValidationError("Invalid duration"))(Function.unlift { str =>
      Try(Duration.parse(str)).toOption
    })
  implicit val durationWrites: Writes[Duration] = Writes { duration =>
    JsString(duration.toString)
  }


}

