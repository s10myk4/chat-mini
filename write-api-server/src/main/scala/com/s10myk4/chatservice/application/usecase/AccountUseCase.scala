package com.s10myk4.chatservice.application.usecase

import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import com.s10myk4.chatservice.adapter.datasource.AccountActor
import com.s10myk4.chatservice.adapter.datasource.AccountActor.CreateAccount
import com.s10myk4.chatservice.application.support.IdGenerator
import com.s10myk4.chatservice.application.usecase.AccountUseCase.AccountUseCaseResult
import com.s10myk4.chatservice.application.usecase.AccountUseCase.input.CreateAccountRequest
import com.s10myk4.chatservice.application.usecase.UseCaseResult.{UseCaseInvalid, UseCaseValid}
import com.s10myk4.chatservice.domain.{Account, AccountId, Member}

import scala.concurrent.{ExecutionContext, Future}

object AccountUseCase {

  object input {

    final case class CreateAccountRequest(name: String) {
      //TODO
      def toEntity(id: Long): Either[InvalidPermission, Account] =
        Right(Account(AccountId(id), name, Member))
    }

  }


  sealed trait AccountUseCaseResult

  final object Valid extends UseCaseValid with AccountUseCaseResult

  sealed trait RoomUseCaseInvalid extends UseCaseInvalid with AccountUseCaseResult

  final case class AlreadyExistAccount(id: AccountId) extends RoomUseCaseInvalid {
    override def message: String = s"Account with id:${id.value} already exist."
  }

  final case class InvalidPermission() extends RoomUseCaseInvalid {
    override def message: String = s"" //TODO
  }

}

class AccountUseCase(
                      idGen: IdGenerator[Long],
                      sharding: ClusterSharding
                    )(implicit ex: ExecutionContext, timeout: Timeout) {

  def createAccount(in: CreateAccountRequest): Future[AccountUseCaseResult] = {
    in.toEntity(idGen.generate()).fold(
      left => Future.successful(left),
      account => {
        val ref = sharding.entityRefFor(AccountActor.entityKey, account.id.value.toString)
        ref.ask(CreateAccount(account, _))
      }
    )
  }

}
