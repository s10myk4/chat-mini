package com.s10myk4.chatservice.adapter.datasource

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import com.s10myk4.chatservice.application.usecase.AccountUseCase
import com.s10myk4.chatservice.application.usecase.AccountUseCase.{CreateAccount, Valid}
import com.s10myk4.chatservice.domain.Account

object AccountActor {

  type Command = AccountUseCase.Command

  sealed trait Event extends CborSerializable

  final case class CreatedAccount(account: Account) extends Event

  sealed trait State

  final case object EmptyState extends State

  final case class JustState(account: Account) extends State

  val entityKey: EntityTypeKey[Command] = EntityTypeKey[Command]("account")

  private val separator = "_"

  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[Command]] = {
    ClusterSharding(system).init(
      Entity(entityKey) { ctx =>
        EventSourcedBehavior(
          persistenceId = PersistenceId(entityKey.name, ctx.entityId, separator),
          emptyState = EmptyState,
          commandHandler = commandHandler(),
          eventHandler = eventHandler
        )
      }
    )
  }

  private def commandHandler(): (State, Command) => Effect[Event, State] = { (state, command) =>
    (state, command) match {
      case (EmptyState, CreateAccount(account, replyTo)) =>
        println("@@ handle CreateAccount Command")
        Effect.persist(CreatedAccount(account)).thenReply(replyTo)(_ => Valid)
      case (JustState(_), CreateAccount(account, replyTo)) =>
        Effect.reply(replyTo)(AccountUseCase.AlreadyExistAccount(account.id))
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
