package com.s10myk4.chatservice.adapter.http

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.s10myk4.chatservice.application.usecase.AccountUseCase.input.CreateAccountRequest
import com.s10myk4.chatservice.application.usecase.RoomUseCase.input.{CreateRoomRequest, PostMessageRequest}
import com.s10myk4.chatservice.application.usecase.{AccountUseCase, RoomUseCase}
import spray.json.DefaultJsonProtocol

object ChatServiceRoutes {

  import DefaultJsonProtocol._

  implicit val postMessageJsonFormat = jsonFormat3(PostMessageRequest)
  implicit val createRoomJsonFormat = jsonFormat1(CreateRoomRequest)
  implicit val createAccountJsonFormat = jsonFormat1(CreateAccountRequest)
}

class ChatServiceRoutes(
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
            onSuccess(accountUseCase.createAccount(in)) {
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
            onSuccess(roomUseCase.postMessage(in)) {
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
            val res = roomUseCase.createRoom(in)
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
