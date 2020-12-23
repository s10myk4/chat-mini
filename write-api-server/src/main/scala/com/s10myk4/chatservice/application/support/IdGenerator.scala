package com.s10myk4.chatservice.application.support

trait IdGenerator[T] {
  def generate(): T
}