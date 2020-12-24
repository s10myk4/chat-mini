package com.s10myk4.chatservice.adapter.datasource

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import com.s10myk4.chatservice.application.usecase.RoomUseCase
import com.s10myk4.chatservice.application.usecase.RoomUseCase.RoomUseCaseResult
import com.s10myk4.chatservice.domain.{Message, Room}

object RoomActor {

  sealed trait Command extends CborSerializable

  final case class PostMessage(message: Message, replyTo: ActorRef[RoomUseCaseResult]) extends Command

  final case class CreateRoom(room: Room, replyTo: ActorRef[RoomUseCaseResult]) extends Command

  sealed trait Event extends CborSerializable

  final case class PostedMessage(message: Message) extends Event

  final case class CreatedRoom(room: Room) extends Event

  sealed trait State

  final case object EmptyState extends State

  final case class JustState(room: Room) extends State

  val entityKey: EntityTypeKey[Command] = EntityTypeKey[Command]("room")

  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[Command]] = {
    ClusterSharding(system).init(
      Entity(entityKey) { ctx =>
        println(s"@@ entityId: ${ctx.entityId}")
        RoomActor(ctx.entityId)
      }
    )
  }

  private val separator = "_"

  def apply(roomId: String): Behavior[Command] = {
    val id = PersistenceId(entityKey.name, roomId, separator)
    println(s"@@ PersistenceId: ${id.id}")
    EventSourcedBehavior(
      persistenceId = id,
      emptyState = EmptyState,
      commandHandler = commandHandler(),
      eventHandler = eventHandler
    )
  }

  private def commandHandler(): (State, Command) => Effect[Event, State] = { (state, command) =>
    (state, command) match {
      case (EmptyState, cmd: CreateRoom) =>
        Effect.persist(CreatedRoom(cmd.room)).thenReply(cmd.replyTo) { _ =>
          println(s"@@ persistent command ${cmd.room}")
          RoomUseCase.Valid
        }
      case (EmptyState, PostMessage(msg, replyTo)) =>
        Effect.reply(replyTo)(RoomUseCase.DoesNotExistRoom(msg.roomId))
      case (JustState(_), CreateRoom(room, replyTo)) =>
        Effect.reply(replyTo)(RoomUseCase.AlreadyExistRoom(room.id))
      //case (JustState(state), PostMessage(msg, _)) => //TODO 対象の投稿者が存在するか
      case (JustState(_), PostMessage(msg, _)) =>
        Effect.persist(PostedMessage(msg))
      case _ =>
        Effect.unhandled
    }
  }

  private val eventHandler: (State, Event) => State = { (state, event) =>
    (state, event) match {
      case (EmptyState, CreatedRoom(room)) =>
        println("@@ CreateRoom Event")
        JustState(room)
      case (JustState(state), PostedMessage(message)) =>
        JustState(state.postMessage(message))
      case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
    }
  }

}
