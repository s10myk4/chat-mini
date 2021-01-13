package com.s10myk4.chatservice.adapter.datasource

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityContext, EntityTypeKey}

object ShardedRoomAggregates {

  val name = "rooms"

  private def entityBehavior(
                              childBehavior: Behavior[RoomPersistentActor.Command],
                              actorName: String
                              //receiveTimeout: FiniteDuration
                            ): EntityContext[RoomPersistentActor.Command] => Behavior[RoomPersistentActor.Command] = { entityContext =>
    Behaviors.setup[RoomPersistentActor.Command] { ctx =>
      val childRef = ctx.spawn(childBehavior, actorName)
      //ctx.setReceiveTimeout(receiveTimeout, RoomPersistent.Idle)
      Behaviors.receiveMessage {
        //case RoomPersistent.Idle =>
        //  entityContext.shard ! ClusterSharding.Passivate(ctx.self)
        //  Behaviors.same
        //case RoomPersistent.Stop =>
        //  ctx.log.debug("> Changed state: Stop")
        //  Behaviors.stopped
        cmd =>
          childRef ! cmd
          Behaviors.same
      }
    }
  }

  val entityTypeKey: EntityTypeKey[RoomPersistentActor.Command] = EntityTypeKey[RoomPersistentActor.Command]("room")

  def initClusterSharding(
                           clusterSharding: ClusterSharding,
                           childBehavior: Behavior[RoomPersistentActor.Command],
                           //receiveTimeout: FiniteDuration
                         ): ActorRef[ShardingEnvelope[RoomPersistentActor.Command]] = {
    val entity = Entity(entityTypeKey)(
      createBehavior = entityBehavior(
        childBehavior,
        RoomAggregates.name
      )
    )

    clusterSharding.init(entity
      //entity.withStopMessage(GroupChatProtocol.Stop)
    )
  }

  def ofProxy(clusterSharding: ClusterSharding): Behavior[RoomPersistentActor.Command] =
    Behaviors.receiveMessage[RoomPersistentActor.Command] { msg =>
      val entityRef = clusterSharding
        .entityRefFor[RoomPersistentActor.Command](entityTypeKey, msg.roomId.value.toString.reverse)
      entityRef ! msg
      Behaviors.same
    }

}
