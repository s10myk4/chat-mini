package com.s10myk4.chatservice.domain

import java.time.LocalDateTime

final case class Message(id: MessageId, roomId: RoomId, sender: AccountId, body: String, createdAt: LocalDateTime)

object Message {
  def apply(id: MessageId, roomId: RoomId, sender: AccountId, body: String): Message = {
    new Message(id, roomId, sender, body, LocalDateTime.now())
  }
}
