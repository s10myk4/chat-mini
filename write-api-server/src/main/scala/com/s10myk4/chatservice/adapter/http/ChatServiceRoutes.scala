package com.s10myk4.chatservice.adapter.http

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.s10myk4.chatservice.application.support.IdGenerator
import com.s10myk4.chatservice.application.usecase.AccountUseCase.input.CreateAccountRequest
import com.s10myk4.chatservice.application.usecase.RoomUseCase.input.{CreateRoomRequest, PostMessageRequest}
import com.s10myk4.chatservice.application.usecase.{AccountUseCase, RoomUseCase}
import com.s10myk4.chatservice.domain._
import spray.json.DefaultJsonProtocol

object ChatServiceRoutes {

  import DefaultJsonProtocol._

  implicit val postMessageJsonFormat = jsonFormat3(PostMessageRequest)
  implicit val createRoomJsonFormat = jsonFormat1(CreateRoomRequest)
  implicit val createAccountJsonFormat = jsonFormat1(CreateAccountRequest)
}

class ChatServiceRoutes(
                         idGen: IdGenerator[Long],
                         roomUseCase: RoomUseCase,
                         accountUseCase: AccountUseCase,
                       )(implicit system: ActorSystem[_], timeout: Timeout) extends SprayJsonSupport {

  import ChatServiceRoutes._

  lazy val topLevel: Route = concat {
    pathPrefix("account") {
      concat(
        post {
          entity(as[CreateAccountRequest]) { in =>
            import AccountUseCase._
            val id = idGen.generate()
            onSuccess(accountUseCase.createAccount(in.toEntity(id))) {
              case Valid =>
                complete(StatusCodes.Created)
              case err: AlreadyExistAccount =>
                complete(StatusCodes.BadRequest -> err.message)
              case err: InvalidPermission =>
                complete(StatusCodes.BadRequest -> err.message)
            }
          }
        }
      )
    }
    pathPrefix("message") {
      concat(
        post {
          entity(as[PostMessageRequest]) { in =>
            import RoomUseCase._
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
          entity(as[CreateRoomRequest]) { in =>
            import RoomUseCase._
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
