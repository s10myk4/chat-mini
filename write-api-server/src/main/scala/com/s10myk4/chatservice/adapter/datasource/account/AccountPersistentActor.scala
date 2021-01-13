package com.s10myk4.chatservice.adapter.datasource.account

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import com.s10myk4.chatservice.adapter.datasource.CborSerializable
import com.s10myk4.chatservice.application.usecase.AccountUseCase
import com.s10myk4.chatservice.application.usecase.AccountUseCase.{AccountUseCaseResult, Valid}
import com.s10myk4.chatservice.domain.{Account, AccountId}

object AccountPersistentActor {

  sealed trait Command extends CborSerializable {
    def accountId: AccountId
  }

  final case class CreateAccount(account: Account, replyTo: ActorRef[AccountUseCaseResult]) extends Command {
    override val accountId: AccountId = account.id
  }

  final case class IsExist[T](accountId: AccountId, replyTo: ActorRef[T]) extends Command

  sealed trait Event extends CborSerializable

  final case class CreatedAccount(account: Account) extends Event

  sealed trait State

  final case object EmptyState extends State

  final case class JustState(account: Account) extends State

  def apply(accountId: AccountId): Behavior[Command] = Behaviors.setup[Command] { ctx =>
    EventSourcedBehavior(
      persistenceId = PersistenceId.of(accountId.value.toString, "-"),
      emptyState = EmptyState,
      commandHandler = commandHandler(),
      eventHandler = eventHandler
    )
  }

  private def commandHandler(): (State, Command) => Effect[Event, State] = { (state, command) =>
    (state, command) match {
      case (EmptyState, CreateAccount(account, replyTo)) =>
        Effect.persist(CreatedAccount(account)).thenReply(replyTo)(_ => Valid)
      case (JustState(_), CreateAccount(account, replyTo)) =>
        Effect.reply(replyTo)(AccountUseCase.AlreadyExistAccount(account.id))
      case _ =>
        Effect.unhandled
    }
  }

  private val eventHandler: (State, Event) => State = { (state, event) =>
    (state, event) match {
      case (EmptyState, CreatedAccount(account)) => JustState(account)
    }
  }

}
