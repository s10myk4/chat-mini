package com.s10myk4.chatservice.application.usecase

import akka.actor.typed.{ActorRef, Scheduler}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import com.s10myk4.chatservice.adapter.datasource.{CborSerializable, RoomActor}
import com.s10myk4.chatservice.application.usecase.RoomUseCase.{CreateRoom, PostMessage, RoomUseCaseResult, Valid}
import com.s10myk4.chatservice.application.usecase.UseCaseResult._
import com.s10myk4.chatservice.domain.{AccountId, Message, Room, RoomId}

import scala.concurrent.{ExecutionContext, Future}

object RoomUseCase {

  sealed trait Command extends CborSerializable

  final case class PostMessage(message: Message, replyTo: ActorRef[RoomUseCaseResult]) extends Command

  final case class CreateRoom(room: Room, replyTo: ActorRef[RoomUseCaseResult]) extends Command

  sealed trait RoomUseCaseResult

  final object Valid extends UseCaseValid with RoomUseCaseResult

  sealed trait RoomUseCaseInvalid extends UseCaseInvalid with RoomUseCaseResult

  final case class DoesNotExistSender(senderId: AccountId) extends RoomUseCaseInvalid {
    override def message: String = s"Sender with ${senderId.value} id does not exist."
  }

  final case class DoesNotExistRoom(roomId: RoomId) extends RoomUseCaseInvalid {
    override def message: String = s"Room with ${roomId.value} id does not exist."
  }

}

class RoomUseCase(sharding: ClusterSharding)(implicit ex: ExecutionContext, timeout: Timeout, scheduler: Scheduler) {

  def postMessage(roomId: RoomId, message: Message): Future[RoomUseCaseResult] = {
    val roomRef = sharding.entityRefFor(RoomActor.entityKey, roomId.value.toString)
    roomRef.ask[RoomUseCase.RoomUseCaseResult](PostMessage(message, _))
      .map(_ => Valid)
  }

  def createRoom(room: Room): Future[RoomUseCaseResult] = {
    val roomRef = sharding.entityRefFor(RoomActor.entityKey, room.id.value.toString)
    roomRef.ask(CreateRoom(room, _)).map(_ => Valid)
  }

}
