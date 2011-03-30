package com.vasilrem.santa.stm

import scala.concurrent.stm._

case class Group(capacity: Int){

  private val gates = Ref((capacity, Gate(capacity), Gate(capacity)))

  def join = atomic { implicit txn =>
    gates() match {
      case (remainingCapacity, inGate, outGate) =>
        if(remainingCapacity == 0) retry
        gates() = (remainingCapacity - 1, inGate, outGate)
        (inGate, outGate)
    }
  }

  def await = atomic { implicit txn =>
    gates() match {
      case (remainingCapacity, inGate, outGate) =>
        if(remainingCapacity > 0) retry
        gates() = (capacity, Gate(capacity), Gate(capacity))
        (inGate, outGate)
    }
  }

}

