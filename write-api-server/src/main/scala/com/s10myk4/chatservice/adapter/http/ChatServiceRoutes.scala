package com.s10myk4.chatservice.adapter.http

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.s10myk4.chatservice.adapter.http.ChatServiceRoutes.{CreateRoom, PostMessage}
import com.s10myk4.chatservice.application.support.IdGenerator
import com.s10myk4.chatservice.application.usecase.RoomUseCase
import com.s10myk4.chatservice.application.usecase.RoomUseCase.{DoesNotExistRoom, DoesNotExistSender, Valid}
import com.s10myk4.chatservice.domain._
import spray.json.DefaultJsonProtocol

object ChatServiceRoutes {

  final case class PostMessage(roomId: Long, sender: Long, body: String) {
    def toEntity(id: Long): Message = Message(MessageId(id), RoomId(roomId), AccountId(sender), body)
  }

  final case class CreateRoom(name: String) {
    def toEntity(id: Long): Room = Room(RoomId(id), name, Set.empty, Vector.empty)
  }

  import DefaultJsonProtocol._

  implicit val postMessageJsonFormat = jsonFormat3(PostMessage)
  implicit val createRoomJsonFormat = jsonFormat1(CreateRoom)
}

class ChatServiceRoutes(
                         idGen: IdGenerator[Long],
                         roomUseCase: RoomUseCase,
                       )(implicit system: ActorSystem[_], timeout: Timeout) extends SprayJsonSupport {

  lazy val topLevel: Route = concat {
    pathPrefix("message") {
      concat(
        post {
          entity(as[PostMessage]) { in =>
            val id = idGen.generate()
            onSuccess(roomUseCase.postMessage(RoomId(in.roomId), in.toEntity(id))) {
              case Valid =>
                complete(StatusCodes.Created)
              case err: DoesNotExistSender =>
                complete(StatusCodes.BadRequest -> err.message)
              case err: DoesNotExistRoom =>
                complete(StatusCodes.BadRequest -> err.message)
            }
          }
        }
      )
    }
    pathPrefix("room") {
      concat(
        post {
          entity(as[CreateRoom]) { in =>
            val id = idGen.generate()
            val res = roomUseCase.createRoom(in.toEntity(id))
            onSuccess(res) {
              case Valid =>
                complete(StatusCodes.Created)
              case _ =>
                complete(StatusCodes.InternalServerError)
            }
          }
        }
      )

    }
  }

}
