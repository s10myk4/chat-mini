package com.s10myk4.chatservice.application.usecase

import akka.actor.typed.{ActorRef, Scheduler}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import com.s10myk4.chatservice.adapter.datasource.{CborSerializable, RoomActor}
import com.s10myk4.chatservice.application.support.IdGenerator
import com.s10myk4.chatservice.application.usecase.RoomUseCase.input.{CreateRoomRequest, PostMessageRequest}
import com.s10myk4.chatservice.application.usecase.RoomUseCase.{CreateRoom, PostMessage, RoomUseCaseResult}
import com.s10myk4.chatservice.application.usecase.UseCaseResult._
import com.s10myk4.chatservice.domain._

import scala.concurrent.{ExecutionContext, Future}

object RoomUseCase {

  object input {

    final case class PostMessageRequest(roomId: Long, sender: Long, body: String) {
      def toEntity(id: Long): Message = Message(MessageId(id), RoomId(roomId), AccountId(sender), body)
    }

    final case class CreateRoomRequest(name: String) {
      def toEntity(id: Long): Room = Room(RoomId(id), name, Set.empty, Vector.empty)
    }

  }

  sealed trait Command extends CborSerializable

  final case class PostMessage(message: Message, replyTo: ActorRef[RoomUseCaseResult]) extends Command

  final case class CreateRoom(room: Room, replyTo: ActorRef[RoomUseCaseResult]) extends Command

  sealed trait RoomUseCaseResult

  final object Valid extends UseCaseValid with RoomUseCaseResult

  sealed trait RoomUseCaseInvalid extends UseCaseInvalid with RoomUseCaseResult

  final case class AlreadyExistRoom(roomId: RoomId) extends RoomUseCaseInvalid {
    override def message: String = s"Room with id:${roomId.value} already exist."
  }

  final case class DoesNotExistSender(senderId: AccountId) extends RoomUseCaseInvalid {
    override def message: String = s"Sender with ${senderId.value} id does not exist."
  }

  final case class DoesNotExistRoom(roomId: RoomId) extends RoomUseCaseInvalid {
    override def message: String = s"Room with ${roomId.value} id does not exist."
  }

}

class RoomUseCase(
                   idGen: IdGenerator[Long],
                   clusterSharding: ClusterSharding
                 )(implicit ex: ExecutionContext, timeout: Timeout, scheduler: Scheduler) {


  def postMessage(in: PostMessageRequest): Future[RoomUseCaseResult] = {
    val message = in.toEntity(idGen.generate())
    val roomRef = clusterSharding.entityRefFor(RoomActor.entityKey, message.roomId.value.toString)
    roomRef.ask[RoomUseCase.RoomUseCaseResult](PostMessage(message, _))
  }

  def createRoom(in: CreateRoomRequest): Future[RoomUseCaseResult] = {
    val room = in.toEntity(idGen.generate())
    val roomRef = clusterSharding.entityRefFor(RoomActor.entityKey, room.id.value.toString)
    roomRef.ask(CreateRoom(room, _))
  }

}
