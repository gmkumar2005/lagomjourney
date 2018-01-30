package CMSOrders.impl

import CMSOrders.api.RegistrantApi
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import com.typesafe.conductr.bundlelib.lagom.scaladsl.ConductRApplicationComponents
import play.api.Environment
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.ExecutionContext

trait OrderComponents extends LagomServerComponents
  with CassandraPersistenceComponents {
  implicit def executionContext: ExecutionContext

  def environment: Environment

  override lazy val lagomServer = serverFor[RegistrantApi](wire[OrderServiceImpl])
  lazy val jsonSerializerRegistry = OrderSerializerRegistry
  persistentEntityRegistry.register(wire[OrderEntity])

}

abstract class OrderApplication(context: LagomApplicationContext) extends LagomApplication(context)
  with OrderComponents
  with AhcWSComponents
  with LagomKafkaComponents {

}

class OrdersApplicationLoader extends LagomApplicationLoader {
  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new OrderApplication(context) with LagomDevModeComponents

  override def load(context: LagomApplicationContext): LagomApplication =
    new OrderApplication(context) with ConductRApplicationComponents

  override def describeService = Some(readDescriptor[RegistrantApi])
}
