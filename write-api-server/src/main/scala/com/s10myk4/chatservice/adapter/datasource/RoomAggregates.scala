package com.s10myk4.chatservice.adapter.datasource

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.s10myk4.chatservice.domain.RoomId

object RoomAggregates {
  val name = "rooms"

  def behavior(
                childName: RoomId => String,
                childBehavior: RoomId => Behavior[RoomPersistentActor.Command],
              ): Behavior[RoomPersistentActor.Command] = {
    Behaviors.setup { ctx =>
      def getOrCreateRef(roomId: RoomId): ActorRef[RoomPersistentActor.Command] = {
        ctx.child(childName(roomId)).fold {
          ctx.log.debug("spawn child actor {}", roomId)
          ctx.spawn(childBehavior(roomId), childName(roomId))
        }(_.asInstanceOf[ActorRef[RoomPersistentActor.Command]])
      }

      Behaviors.receiveMessage[RoomPersistentActor.Command] { cmd =>
        getOrCreateRef(cmd.roomId) ! cmd
        Behaviors.same
      }
    }
  }

}
