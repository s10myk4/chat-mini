package com.s10myk4.chatservice.adapter.datasource.account

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.s10myk4.chatservice.domain.AccountId

object AccountAggregates {
  val name = "account"

  def behavior(
                childName: AccountId => String,
                childBehavior: AccountId => Behavior[AccountPersistentActor.Command],
              ): Behavior[AccountPersistentActor.Command] = {
    Behaviors.setup { ctx =>
      def getOrCreateRef(accountId: AccountId): ActorRef[AccountPersistentActor.Command] = {
        ctx.child(childName(accountId)).fold {
          ctx.log.debug("spawn child actor {}", accountId)
          ctx.spawn(childBehavior(accountId), childName(accountId))
        }(_.asInstanceOf[ActorRef[AccountPersistentActor.Command]])
      }

      Behaviors.receiveMessage[AccountPersistentActor.Command] { cmd =>
        getOrCreateRef(cmd.accountId) ! cmd
        Behaviors.same
      }
    }
  }
}
