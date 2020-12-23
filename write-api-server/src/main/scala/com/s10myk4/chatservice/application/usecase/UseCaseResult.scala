package com.s10myk4.chatservice.application.usecase

object UseCaseResult {
  sealed trait UseCaseResult

  trait UseCaseValid extends UseCaseResult

  trait UseCaseInvalid extends UseCaseResult {
    def message: String
  }
}
