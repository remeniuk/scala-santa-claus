/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vasilrem.santa.actors

import com.vasilrem.santa.stm.TaskScheduler
import scala.actors.{Actor, TIMEOUT, Futures}

case object Order

sealed trait Helper extends Actor{
  val id: Int
  def job: Unit

  def randomDelay = math.random * 10000 toLong

  def act = loop{react{
      case Order => job; TaskScheduler.schedule({
            reply(Idle(this)); Secretary ! Idle(this)
          }, randomDelay)
    }}

}
class Elf(val id: Int) extends Helper{
  def job = println("Elf %s meeting in the study." format(id))
}
class Reindeer(val id: Int) extends Helper{
  def job = println("Reindeer %s delivering toys." format(id))
}

case class Group[T <% Helper](capacity: Int, helpers: List[T] = List()){
  def hasSpace = helpers.size < capacity
}

object Santa extends Actor{
  
  def giveOrderAndWait[T <% Helper](helpers: List[Helper]) = {
    println("\r\nHo! Ho! Ho! Let's %s!" format(
        if(helpers.isInstanceOf[List[Reindeer]]) "deliver toys"
        else "meet in my study"))
    Futures.awaitAll(20000, helpers.map(_ !! Order):_*)
  }
  
  def act = loop { reactWithin(0){
      case Group(_, helpers: List[Reindeer]) => giveOrderAndWait(helpers)
      case TIMEOUT => react {
          case Group(_, helpers: List[Helper]) => giveOrderAndWait(helpers)
        }
    }}

}

case class Idle(helper: Actor)

object Secretary extends Actor{

  def act = loop(Group[Elf](3), Group[Reindeer](9))

  private def addToGroup[T <% Helper](helper: T, group: Group[T]) = {
    val updatedGroup = group.copy(helpers = helper :: group.helpers)
    if(updatedGroup.hasSpace) updatedGroup else {
      Santa ! updatedGroup
      group.copy(helpers = List[T]())
    }
  }

  private def loop(elves: Group[Elf], reindeers: Group[Reindeer]):Unit = {
    react {
      case Idle(reindeer: Reindeer) => loop(elves, addToGroup(reindeer, reindeers))
      case Idle(elf: Elf) => loop(addToGroup(elf, elves), reindeers)
    }
  }

}

object SantaClausSimulation {

  def start = {
    Santa.start
    Secretary.start
    (1 to 9).map(new Reindeer(_).start).foreach(Secretary ! Idle(_))
    (1 to 10).map(new Elf(_).start).foreach(Secretary ! Idle(_))    
  }

}
