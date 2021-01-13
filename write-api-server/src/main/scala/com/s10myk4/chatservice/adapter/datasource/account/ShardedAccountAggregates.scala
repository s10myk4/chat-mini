package com.s10myk4.chatservice.adapter.datasource.account

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityContext, EntityTypeKey}

object ShardedAccountAggregates {
  val name = "accounts"

  private def entityBehavior(
                              childBehavior: Behavior[AccountPersistentActor.Command],
                              actorName: String
                            ): EntityContext[AccountPersistentActor.Command] => Behavior[AccountPersistentActor.Command] = { _ =>
    Behaviors.setup[AccountPersistentActor.Command] { ctx =>
      val childRef = ctx.spawn(childBehavior, actorName)
      Behaviors.receiveMessage {
        cmd =>
          childRef ! cmd
          Behaviors.same
      }
    }
  }

  val entityTypeKey: EntityTypeKey[AccountPersistentActor.Command] =
    EntityTypeKey[AccountPersistentActor.Command]("account")

  def initClusterSharding(
                           clusterSharding: ClusterSharding,
                           childBehavior: Behavior[AccountPersistentActor.Command],
                         ): ActorRef[ShardingEnvelope[AccountPersistentActor.Command]] = {
    val entity = Entity(entityTypeKey)(
      createBehavior = entityBehavior(
        childBehavior,
        AccountAggregates.name
      )
    )
    clusterSharding.init(entity)
  }

  def ofProxy(clusterSharding: ClusterSharding): Behavior[AccountPersistentActor.Command] =
    Behaviors.receiveMessage[AccountPersistentActor.Command] { msg =>
      val entityRef = clusterSharding
        .entityRefFor[AccountPersistentActor.Command](entityTypeKey, msg.accountId.value.toString.reverse)
      entityRef ! msg
      Behaviors.same
    }

}
