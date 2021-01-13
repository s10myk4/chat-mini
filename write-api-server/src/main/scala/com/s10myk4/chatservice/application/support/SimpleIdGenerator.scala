package com.s10myk4.chatservice.application.support

import scala.util.Random

final class SimpleIdGenerator extends IdGenerator[Long] {
  override def generate(): Long = Random.between(1, Long.MaxValue)
}
