package com.s10myk4.chatservice.domain

final case class Account(id: AccountId, name: String, permission: Permission)

sealed trait Permission

case object Admin extends Permission

case object Member extends Permission