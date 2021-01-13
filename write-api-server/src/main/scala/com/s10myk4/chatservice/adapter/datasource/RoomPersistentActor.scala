package com.s10myk4.chatservice.adapter.datasource

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import com.s10myk4.chatservice.adapter.datasource.RoomPersistentActor.Command.{AddMembers, CreateRoom, PostMessage}
import com.s10myk4.chatservice.adapter.datasource.RoomPersistentActor.Event.{AddedMembers, CreatedRoom, PostedMessage}
import com.s10myk4.chatservice.application.usecase.RoomUseCase
import com.s10myk4.chatservice.application.usecase.RoomUseCase.RoomUseCaseResult
import com.s10myk4.chatservice.domain.{AccountId, Message, Room, RoomId}

object RoomPersistentActor {

  sealed trait Command extends CborSerializable {
    def roomId: RoomId
  }

  object Command {

    case class PostMessage(message: Message, replyTo: ActorRef[RoomUseCaseResult]) extends Command {
      val roomId: RoomId = message.roomId
    }

    case class CreateRoom(room: Room, replyTo: ActorRef[RoomUseCaseResult]) extends Command {
      val roomId: RoomId = room.id
    }

    case class AddMembers(roomId: RoomId, members: Set[AccountId], replyTo: ActorRef[RoomUseCaseResult]) extends Command

  }

  sealed trait Event extends CborSerializable

  object Event {

    final case class PostedMessage(roomId: RoomId, message: Message) extends Event

    final case class CreatedRoom(roomId: RoomId, name: String, members: Set[AccountId]) extends Event

    final case class AddedMembers(roomId: RoomId, members: Set[AccountId]) extends Event

  }

  sealed trait State

  final case object EmptyState extends State

  final case class JustState(room: Room) extends State

  //val entityKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Room")

  //def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[Command]] = {
  //  ClusterSharding(system).init(
  //    Entity(entityKey) { ctx =>
  //      RoomPersistent(ctx.entityId)
  //    }
  //  )
  //}

  def apply(roomId: RoomId): Behavior[Command] = Behaviors.setup { _ =>
    EventSourcedBehavior(
      persistenceId = PersistenceId.of(roomId.value.toString, "-"),
      emptyState = EmptyState,
      commandHandler = commandHandler(),
      eventHandler = eventHandler
    )
  }

  private def commandHandler(): (State, Command) => Effect[Event, State] = { (state, command) =>
    (state, command) match {
      case (EmptyState, CreateRoom(room, replyTo)) =>
        println("@@ EmptyState CreateRoom")
        Effect.persist(CreatedRoom(room.id, room.name, room.members))
          .thenReply(replyTo)(_ => RoomUseCase.Valid)
      case (EmptyState, AddMembers(roomId, _, replyTo)) =>
        println("@@ EmptyState AddMembers")
        Effect.reply(replyTo)(RoomUseCase.DoesNotExistRoom(roomId))
      case (EmptyState, PostMessage(msg, replyTo)) =>
        println("@@ EmptyState PostMessage")
        Effect.reply(replyTo)(RoomUseCase.DoesNotExistRoom(msg.roomId))
      //Effect.persist(PostedMessage(msg.roomId, msg)).thenReply(replyTo)(_ => RoomUseCase.Valid)
      case (JustState(_), CreateRoom(room, replyTo)) =>
        Effect.reply(replyTo)(RoomUseCase.AlreadyExistRoom(room.id))
      //case (JustState(state), PostMessage(msg, _)) => //TODO 対象の投稿者が存在するか
      case (JustState(state), PostMessage(msg, replyTo)) =>
        println("@@ JustState PostMessage")
        if (state.isMember(msg.sender)) {
          Effect.persist(PostedMessage(msg.roomId, msg)).thenReply(replyTo)(_ => RoomUseCase.Valid)
        } else {
          Effect.reply(replyTo)(RoomUseCase.SenderIsNotMemberOfRoom(msg.sender, msg.roomId))
        }
      case (JustState(_), AddMembers(roomId, members, replyTo)) =>
        println("@@ JustState AddMembers")
        Effect.persist(AddedMembers(roomId, members)).thenReply(replyTo)(_ => RoomUseCase.Valid)
      case _ =>
        Effect.unhandled
    }
  }

  private val eventHandler: (State, Event) => State = { (state, event) =>
    (state, event) match {
      case (EmptyState, CreatedRoom(roomId, name, members)) =>
        println(s"CreateRoom Event (roomId: ${roomId.value})")
        JustState(Room.init(roomId, name, members))
      case (JustState(state), AddedMembers(_, members)) =>
        println(s"AddedMembers Event (memberIds: ${members.map(_.value).mkString(",")})")
        JustState(state.addMembers(members))
      case (JustState(state), PostedMessage(_, message)) =>
        println(s"PostedMessage Event (messageId: ${message.id.value})")
        JustState(state.postMessage(message))
    }
  }

}
