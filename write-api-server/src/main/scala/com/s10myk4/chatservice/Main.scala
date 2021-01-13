package com.s10myk4.chatservice

import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import akka.actor.typed.{ActorSystem, Scheduler}
import akka.actor.{ActorSystem => ClassicActorSystem}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.s10myk4.chatservice.adapter.datasource.account.{AccountAggregates, AccountPersistentActor, ShardedAccountAggregates}
import com.s10myk4.chatservice.adapter.datasource.{RoomAggregates, RoomPersistentActor, ShardedRoomAggregates}
import com.s10myk4.chatservice.adapter.http.WriteApiServerRoutes
import com.s10myk4.chatservice.application.support.SimpleIdGenerator
import com.s10myk4.chatservice.application.usecase.{AccountUseCase, RoomUseCase}
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object Main extends App {

  private val logger = LoggerFactory.getLogger(Main.getClass)

  private val hostname = "localhost"

  args.headOption match {
    case Some(portString) if portString.matches("""\d+""") =>
      val port = portString.toInt
      val httpPort = ("80" + portString.takeRight(2)).toInt
      setUp(hostname, port, httpPort)
    case None =>
      setUp(hostname, 2551, 9000)
  }

  private def config(port: Int, httpPort: Int): Config = {
    logger.info(s"port: $port / htpPort: $httpPort")
    ConfigFactory.parseString(
      s"""
      akka.remote.artery.canonical.port = $port
      chat-service.http.port = $httpPort
       """).withFallback(ConfigFactory.load())
  }

  private def setUp(host: String, port: Int, httpPort: Int): Unit = {
    val classicSystem: ClassicActorSystem = ClassicActorSystem("chat-mini-write-api", config(port, httpPort))
    import classicSystem.dispatcher

    implicit val system: ActorSystem[Nothing] = classicSystem.toTyped
    implicit val sd: Scheduler = system.scheduler
    implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("chat-service.ask-timeout"))

    val clusterSharding = ClusterSharding(system)

    val roomChildBehavior = RoomAggregates.behavior(_.value.toString, RoomPersistentActor(_))
    ShardedRoomAggregates.initClusterSharding(clusterSharding, roomChildBehavior)
    val roomRef = classicSystem.spawn(ShardedRoomAggregates.ofProxy(clusterSharding), ShardedRoomAggregates.name)

    val accountChildBehavior = AccountAggregates.behavior(_.value.toString, AccountPersistentActor(_))
    ShardedAccountAggregates.initClusterSharding(clusterSharding, accountChildBehavior)
    val accountRef = classicSystem.spawn(ShardedAccountAggregates.ofProxy(clusterSharding), ShardedAccountAggregates.name)

    val idGen = new SimpleIdGenerator
    val roomUseCase = new RoomUseCase(idGen, roomRef)
    val accountUseCase = new AccountUseCase(idGen, accountRef)
    val routes = new WriteApiServerRoutes(roomUseCase, accountUseCase)

    startHttpServer(routes.topLevel, host, httpPort)

    sys.addShutdownHook {
      classicSystem.terminate()
      system.terminate()
    }
  }


  private def startHttpServer(routes: Route, host: String, port: Int)(implicit system: ActorSystem[_], ec: ExecutionContext): Unit = {
    Http().newServerAt(host, port).bind(routes)
      .map(_.addToCoordinatedShutdown(3.seconds))
      .onComplete {
        case Success(binding) =>
          val address = binding.localAddress
          system.log.info("Start server http://{}:{}/", address.getHostString, address.getPort)
        case Failure(ex) =>
          system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
          system.terminate()
      }

  }

}