package com.s10myk4.chatservice

import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.s10myk4.chatservice.adapter.datasource.RoomActor
import com.s10myk4.chatservice.adapter.http.ChatServiceRoutes
import com.s10myk4.chatservice.application.support.SimpleIdGenerator
import com.s10myk4.chatservice.application.usecase.{AccountUseCase, RoomUseCase}
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object Main {

  def main(args: Array[String]): Unit = {
    args.headOption match {
      case Some(portString) if portString.matches("""\d+""") =>
        val port = portString.toInt
        val httpPort = ("80" + portString.takeRight(2)).toInt
        //ActorSystem[Nothing](setUp("localhost", 9000), "ChatService")
        ActorSystem[Nothing](setUp("localhost", httpPort), "ChatService", config(port, httpPort))
      case None =>
        ActorSystem[Nothing](setUp("localhost", 9000), "ChatService", config(2551, 9000))
      //throw new IllegalArgumentException("port number, or cassandra required argument")
    }
  }

  private def config(port: Int, httpPort: Int): Config = {
    println(s"@@ port: $port / htpPort: $httpPort")
    ConfigFactory.parseString(
      s"""
      akka.remote.artery.canonical.port = $port
      chat-service.http.port = $httpPort
       """).withFallback(ConfigFactory.load())
  }

  import akka.actor.typed.scaladsl._

  private def setUp(host: String, port: Int): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
    implicit val system = context.system
    import system.executionContext

    implicit val timeout = Timeout.create(system.settings.config.getDuration("chat-service.ask-timeout"))

    RoomActor.init(system)
    val clusterSharding = ClusterSharding(system)

    val idGen = new SimpleIdGenerator
    val roomUseCase = new RoomUseCase(idGen, clusterSharding)
    val accountUseCase = new AccountUseCase(idGen, clusterSharding)
    val routes = new ChatServiceRoutes(roomUseCase, accountUseCase)

    startHttpServer(routes.topLevel, host, port)(system)
    Behaviors.empty
  }


  private def startHttpServer(routes: Route, host: String, port: Int)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext

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