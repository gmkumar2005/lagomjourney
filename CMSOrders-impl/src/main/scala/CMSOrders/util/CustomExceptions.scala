package CMSOrders.util

import com.lightbend.lagom.scaladsl.api.transport.{TransportErrorCode, TransportException}

case class InvalidIdentifierException(message: String) extends TransportException(TransportErrorCode.PolicyViolation, message)