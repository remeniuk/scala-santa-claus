package com.vasilrem.santa.stm

import scala.concurrent.stm._

case class Gate(capacity: Int, remainingCapacity: Ref[Int] = Ref(0)){

  def open = {
    remainingCapacity.single.swap(capacity)
    atomic { implicit txn =>
      val left = remainingCapacity()
      if(left > 0) retry
    }
  }

  def pass = atomic { implicit txn =>
    val left = remainingCapacity()
    if(left == 0) retry
    remainingCapacity() = left - 1
  }

}
