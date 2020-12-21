package com.s10myk4.chatservice.domain

import java.time.LocalDateTime

final case class Message(id: MessageId, roomId: RoomId, sender: AccountId, body: String, createdAt: LocalDateTime)

object Message {
  def create(id: MessageId, roomId: RoomId, sender: AccountId, body: String): Message = {
    ???
  }
}
