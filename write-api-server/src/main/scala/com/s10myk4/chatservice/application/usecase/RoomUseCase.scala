package com.s10myk4.chatservice.application.usecase

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, Scheduler}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.util.Timeout
import com.s10myk4.chatservice.adapter.datasource.RoomPersistentActor
import com.s10myk4.chatservice.adapter.datasource.RoomPersistentActor.Command.{CreateRoom, PostMessage}
import com.s10myk4.chatservice.application.support.IdGenerator
import com.s10myk4.chatservice.application.usecase.RoomUseCase.RoomUseCaseResult
import com.s10myk4.chatservice.application.usecase.RoomUseCase.input.{CreateRoomRequest, PostMessageRequest}
import com.s10myk4.chatservice.application.usecase.UseCaseResult._
import com.s10myk4.chatservice.domain._

import scala.concurrent.Future

object RoomUseCase {

  object input {

    final case class PostMessageRequest(roomId: Long, sender: Long, body: String) {
      def toEntity(id: Long): Message = Message(MessageId(id), RoomId(roomId), AccountId(sender), body)
    }

    final case class CreateRoomRequest(name: String) {
      def toEntity(id: Long): Room = Room(RoomId(id), name, Set.empty, Vector.empty)
    }

  }


  sealed trait RoomUseCaseResult

  final object Valid extends UseCaseValid with RoomUseCaseResult

  sealed trait RoomUseCaseInvalid extends UseCaseInvalid with RoomUseCaseResult

  final case class AlreadyExistRoom(roomId: RoomId) extends RoomUseCaseInvalid {
    override def message: String = s"Room with id:${roomId.value} already exist."
  }

  final case class SenderIsNotMemberOfRoom(senderId: AccountId, roomId: RoomId) extends RoomUseCaseInvalid {
    override def message: String = s"Sender with ${senderId.value} id is not a member of the room(${roomId.value})."
  }

  final case class DoesNotExistRoom(roomId: RoomId) extends RoomUseCaseInvalid {
    override def message: String = s"Room with ${roomId.value} id does not exist."
  }

}

class RoomUseCase(
                   idGen: IdGenerator[Long],
                   //roomRef: ActorRef[RoomPersistentActor.Command],
                   roomRef: ActorRef[RoomPersistentActor.Command],
                   //clusterSharding: ClusterSharding
                 )(implicit timeout: Timeout, scheduler: Scheduler) {


  def postMessage(in: PostMessageRequest): Future[RoomUseCaseResult] = {
    val message = in.toEntity(idGen.generate())
    roomRef.ask[RoomUseCase.RoomUseCaseResult](res => PostMessage(message, res))
  }

  def createRoom(in: CreateRoomRequest): Future[RoomUseCaseResult] = {
    val room = in.toEntity(idGen.generate())
    roomRef.ask(CreateRoom(room, _))
  }

}
