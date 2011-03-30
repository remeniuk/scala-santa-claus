package com.vasilrem.santa.stm

import scala.concurrent.stm._
import scala.actors._
import scheduler.ResizableThreadPoolScheduler
import Actor._

sealed trait SantaLifecycle
case object WakeUp extends SantaLifecycle
case object CallToArms extends SantaLifecycle

object Santa extends Actor {

  Scheduler.impl = {
    val s = new ResizableThreadPoolScheduler(false)
    s.start
    s
  }

  private val elfGroup = Group(3)
  private val reinGroup = Group(9)

  def act = loop{
    react{
      case CallToArms =>
        (1 to 10) map(new Elf(_, elfGroup)) foreach(_.start ! AskForOrders)
        (1 to 9) map(new Reindeer(_, reinGroup)) foreach(_.start ! AskForOrders)

      case WakeUp =>
        atomic { implicit txn =>
          (reinGroup.await, "deliver toys")
        } orAtomic { implicit txn =>
          (elfGroup.await, "meet in my study")
        } match {
          case ((inGate, outGate), task) =>
            println("\r\nHo! Ho! Ho! Let's %s " + task)
            inGate.open; outGate.open
        }
    }
  }

  start

}