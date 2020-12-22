package com.s10myk4.chatservice.adapter.datasource

import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import com.s10myk4.chatservice.application.usecase.RoomUseCase
import com.s10myk4.chatservice.domain.{Message, Room}

object RoomPersistentBehavior {

  type Command = RoomUseCase.Command

  sealed trait Event extends CborSerializable

  final case class PostedMessage(message: Message) extends Event

  final case class CreatedRoom(room: Room) extends Event

  sealed trait State

  final case object EmptyState extends State

  final case class JustState(room: Room) extends State

  val entityKey: EntityTypeKey[Command] = EntityTypeKey[Command]("room")

  def init(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(entityKey) { ctx =>
      RoomPersistentBehavior(ctx.entityId)
    }.withRole("room"))
  }

  def apply(roomId: String): Behavior[Command] = {
    EventSourcedBehavior(
      persistenceId = PersistenceId(entityKey.name, roomId),
      emptyState = EmptyState,
      commandHandler = commandHandler(),
      eventHandler = eventHandler
    )
  }

  private def commandHandler(): (State, Command) => Effect[Event, State] = { (state, command) =>
    (state, command) match {
      case (EmptyState, cmd: RoomUseCase.CreateRoom) =>
        Effect.persist(CreatedRoom(cmd.room)).thenReply(cmd.replyTo) { _ =>
          println(s"@@ persistent command ${cmd.room}")
          RoomUseCase.Valid
        }
      case (JustState(_), cmd: RoomUseCase.PostMessage) =>
        Effect.persist(PostedMessage(cmd.message))
      case _ =>
        Effect.unhandled
    }
  }

  private val eventHandler: (State, Event) => State = { (state, event) =>
    (state, event) match {
      case (EmptyState, CreatedRoom(room)) => {
        println("@@ CreateRoom Event")
        JustState(room)
      }
      case (JustState(state), PostedMessage(message)) => JustState(state.postMessage(message))
      case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
    }
  }

}
