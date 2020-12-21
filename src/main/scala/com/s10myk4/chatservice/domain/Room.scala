package com.s10myk4.chatservice.domain

final case class Room(id: RoomId, name: String, members: Set[AccountId], messages: Vector[Message]) {
  def addMember(member: AccountId): Room = copy(members = members + member)

  def postMessage(message: Message): Room = copy(messages = messages.appended(message))
}

object Room {
  def init(id: RoomId, name: String): Room = Room(id, name, Set.empty)

  def apply(id: RoomId, name: String, members: Set[AccountId]): Room = Room(id, name, members)
}