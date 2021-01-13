package com.s10myk4.chatservice.domain

final case class Room(id: RoomId, name: String, members: Set[AccountId], messages: Vector[Message]) {
  def addMembers(newMembers: Set[AccountId]): Room = copy(members = members ++ newMembers)

  def postMessage(message: Message): Room = copy(messages = messages.appended(message))

  def isMember(senderId: AccountId): Boolean = {
    //members.contains(senderId)
    true
  }
}

object Room {
  def init(id: RoomId, name: String, members: Set[AccountId]): Room =
    Room(id, name, members, Vector.empty)

}