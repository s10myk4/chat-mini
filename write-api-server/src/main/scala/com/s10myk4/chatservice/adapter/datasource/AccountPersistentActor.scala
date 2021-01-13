package com.s10myk4.chatservice.adapter.datasource

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import com.s10myk4.chatservice.application.usecase.AccountUseCase.{AccountUseCaseResult, AlreadyExistAccount, Valid}
import com.s10myk4.chatservice.domain.{Account, AccountId, RoomId}

object AccountPersistentActor {

  sealed trait Command extends CborSerializable

  final case class CreateAccount(account: Account, replyTo: ActorRef[AccountUseCaseResult]) extends Command

  final case class IsExist[T](id: AccountId, replyTo: ActorRef[T]) extends Command

  sealed trait Event extends CborSerializable

  final case class CreatedAccount(account: Account) extends Event

  sealed trait State

  final case object EmptyState extends State

  final case class JustState(account: Account) extends State

  val entityKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Account")

  private val separator = "_"

  def init(roomId: RoomId): Behavior[Command] = Behaviors.setup[Command] { ctx =>
    EventSourcedBehavior(
      persistenceId = PersistenceId.of(roomId.value.toString, separator),
      emptyState = EmptyState,
      commandHandler = commandHandler(),
      eventHandler = eventHandler
    )
  }

  private def commandHandler(): (State, Command) => Effect[Event, State] = { (state, command) =>
    (state, command) match {
      case (EmptyState, CreateAccount(account, replyTo)) =>
        println("@@ handle CreateAccount Command")
        Effect.persist(CreatedAccount(account)).thenReply(replyTo)(_ => Valid)
      case (JustState(_), CreateAccount(account, replyTo)) =>
        Effect.reply(replyTo)(AlreadyExistAccount(account.id))
      case _ =>
        Effect.unhandled
    }
  }

  private val eventHandler: (State, Event) => State = { (state, event) =>
    (state, event) match {
      case (EmptyState, CreatedAccount(account)) =>
        println("@@ CreatedAccount Event")
        JustState(account)
      case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
    }
  }

}
