package com.s10myk4.chatservice.application.usecase

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, Scheduler}
import akka.util.Timeout
import com.s10myk4.chatservice.adapter.datasource.account.AccountPersistentActor
import com.s10myk4.chatservice.adapter.datasource.account.AccountPersistentActor.CreateAccount
import com.s10myk4.chatservice.application.support.IdGenerator
import com.s10myk4.chatservice.application.usecase.AccountUseCase.AccountUseCaseResult
import com.s10myk4.chatservice.application.usecase.AccountUseCase.input.CreateAccountRequest
import com.s10myk4.chatservice.application.usecase.UseCaseResult.{UseCaseInvalid, UseCaseValid}
import com.s10myk4.chatservice.domain.{Account, AccountId, Member}

import scala.concurrent.Future

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
                      accountRef: ActorRef[AccountPersistentActor.Command]
                    )(implicit timeout: Timeout, scheduler: Scheduler) {

  def createAccount(in: CreateAccountRequest): Future[AccountUseCaseResult] = {
    in.toEntity(idGen.generate()).fold(
      left => Future.successful(left),
      account => accountRef.ask(CreateAccount(account, _))
    )
  }

}
