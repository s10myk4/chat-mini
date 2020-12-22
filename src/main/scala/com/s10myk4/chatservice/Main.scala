package com.s10myk4.chatservice

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.s10myk4.chatservice.adapter.datasource.RoomPersistentBehavior
import com.s10myk4.chatservice.adapter.http.ChatServiceRoutes
import com.s10myk4.chatservice.application.support.SimpleIdGenerator
import com.s10myk4.chatservice.application.usecase.RoomUseCase
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
        throw new IllegalArgumentException("port number, or cassandra required argument")

    }
  }

  private def config(port: Int, httpPort: Int): Config = {
    println(s"@@ port: $port / htpPort: $httpPort")
    ConfigFactory.parseString(s"""
      akka.remote.artery.canonical.port = $port
      chat-service.http.port = $httpPort
       """).withFallback(ConfigFactory.load())
  }

  private def setUp(host: String, port: Int): Behavior[Nothing] = {
    Behaviors.setup[Nothing] { context =>
      implicit val system: ActorSystem[Nothing] = context.system
      import system.executionContext
      implicit val timeout = Timeout.create(system.settings.config.getDuration("chat-service.ask-timeout"))
      implicit val sc = system.scheduler
      val sharding: ClusterSharding = ClusterSharding(system)
      //val settings = EventProcessorSettings(system)
      //val httpPort = context.system.settings.config.getInt("shopping.http.port")
      RoomPersistentBehavior.init(system)
      val idGen = new SimpleIdGenerator
      val useCase = new RoomUseCase(sharding)
      val routes = new ChatServiceRoutes(idGen, useCase)
      startHttpServer(routes.topLevel, host, port)
      Behaviors.empty
    }
  }


  private def startHttpServer(routes: Route, host: String, port: Int)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext

    Http().newServerAt(host, port).bind(routes)
      .map(_.addToCoordinatedShutdown(3.seconds))
      .onComplete {
        case Success(binding) =>
          val address = binding.localAddress
          system.log.info("Shopping online at http://{}:{}/", address.getHostString, address.getPort)
        case Failure(ex) =>
          system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
          system.terminate()
      }
  }

  /*
  private def setUp(host: String, port: Int): Behavior[Message] = Behaviors.setup { ctx =>
    implicit val system = ctx.system
    implicit val timeout = Timeout.create(system.settings.config.getDuration("chat-service.ask-timeout"))
    implicit val sc = system.scheduler

    //TODO
    import scala.concurrent.ExecutionContext.Implicits.global
    import akka.actor.typed.scaladsl._

    val bh = RoomPersistentBehavior.apply()
    val idGen = new SimpleIdGenerator
    val useCase = new RoomUseCase(bh, ctx)
    val routes = new ChatServiceRoutes(idGen, useCase)

    val serverBinding = Http().newServerAt(host, port).bind(routes.topLevel)
    ctx.pipeToSelf(serverBinding) {
      case Success(binding) => Started(binding)
      case Failure(ex) => StartFailed(ex)
    }

    def running(binding: ServerBinding): Behavior[Message] =
      Behaviors.receiveMessagePartial[Message] {
        case Stop =>
          ctx.log.info("Stopping server http://{}:{}/",
            binding.localAddress.getHostString, binding.localAddress.getPort)
          Behaviors.stopped
      }.receiveSignal {
        case (_, PostStop) =>
          binding.unbind()
          Behaviors.same
      }

    def starting(wasStopped: Boolean): Behaviors.Receive[Message] =
      Behaviors.receiveMessage[Message] {
        case StartFailed(cause) => throw new RuntimeException("Server failed to start", cause)
        case Started(binding) =>
          ctx.log.info("Server online at http://{}:{}/",
            binding.localAddress.getHostString, binding.localAddress.getPort)
          if (wasStopped) ctx.self ! Stop
          running(binding)
        case Stop =>
          starting(wasStopped = true)
      }

    starting(wasStopped = false)
  }
   */

}