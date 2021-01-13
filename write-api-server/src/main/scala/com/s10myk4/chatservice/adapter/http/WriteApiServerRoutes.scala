package com.s10myk4.chatservice.adapter.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.s10myk4.chatservice.application.usecase.AccountUseCase.input.CreateAccountRequest
import com.s10myk4.chatservice.application.usecase.RoomUseCase.input.{CreateRoomRequest, PostMessageRequest}
import com.s10myk4.chatservice.application.usecase.{AccountUseCase, RoomUseCase}
import spray.json.DefaultJsonProtocol

object WriteApiServerRoutes {

  import DefaultJsonProtocol._

  implicit val postMessageJsonFormat = jsonFormat3(PostMessageRequest)
  implicit val createRoomJsonFormat = jsonFormat1(CreateRoomRequest)
  implicit val createAccountJsonFormat = jsonFormat1(CreateAccountRequest)
}

class WriteApiServerRoutes(
                         roomUseCase: RoomUseCase,
                         accountUseCase: AccountUseCase,
                       ) extends SprayJsonSupport {

  import WriteApiServerRoutes._

  lazy val messagePath: Route = pathPrefix("message") {
    post {
      entity(as[PostMessageRequest]) { in =>
        import RoomUseCase._
        onSuccess(roomUseCase.postMessage(in)) {
          case Valid =>
            complete(StatusCodes.Created)
          case err: AlreadyExistRoom =>
            complete(StatusCodes.BadRequest -> err.message)
          case err: SenderIsNotMemberOfRoom =>
            complete(StatusCodes.BadRequest -> err.message)
          case err: DoesNotExistRoom =>
            complete(StatusCodes.BadRequest -> err.message)
          case _ =>
            complete(StatusCodes.InternalServerError)
        }
      }
    }
  }

  lazy val roomPath: Route =
    pathPrefix("room") {
      post {
        entity(as[CreateRoomRequest]) { in =>
          import RoomUseCase._
          onSuccess(roomUseCase.createRoom(in)) {
            case Valid =>
              complete(StatusCodes.Created)
            case _ =>
              complete(StatusCodes.InternalServerError)
          }
        }
      }
    }

  lazy val accountPath: Route =
    pathPrefix("account") {
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
    }

  lazy val topLevel: Route = concat(messagePath, roomPath, accountPath)

}
