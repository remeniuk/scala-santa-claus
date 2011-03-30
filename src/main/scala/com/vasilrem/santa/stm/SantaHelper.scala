package com.vasilrem.santa.stm

import scala.actors.Actor

sealed trait SantaHelperLifecycle
case object AskForOrders extends SantaHelperLifecycle

trait SantaHelper extends Actor{
  val id: Int
  val group: Group
  val task: () => Any

  def randomDelay = math.random * 10000 toLong

  def act = loop {
    react {
      case AskForOrders =>
        group.join match {
          case (inGate, outGate) =>
            inGate.pass; task(); outGate.pass
            TaskScheduler.schedule({this ! AskForOrders}, randomDelay)
        }
    }
  }

}

class Elf(_id: Int, _group: Group) extends SantaHelper{
  val id = _id
  val group = _group
  val task = () => println("Elf %s meeting in the study" format(id))
}

class Reindeer(_id: Int, _group: Group) extends SantaHelper{
  val id = _id
  val group = _group
  val task = () => println("Reindeer %s delivering toys" format(id))
}
