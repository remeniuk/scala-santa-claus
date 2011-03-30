/*
 *
 * THE SANTA CLAUS PROBLEM:
 *
 * Santa Claus sleeps at the North pole until awakened by either all
 * of the nine reindeer, or by a group of three out of ten elves. He
 * performs one of two indivisible actions:
 *
 *       - If awakened by the group of reindeer, Santa harnesses them to
 *       a sleigh, delivers toys, and finally unharnesses the reindeer who
 *       then go on vacation.
 *       - If awakened by a group of elves, Santa shows them into his
 *       office, consults with them on toy R&D, and finally shows them
 *       out so they can return to work constructing toys.
 *
 * A waiting group of reindeer must be served by Santa before a waiting
 * group of elves. Since Santa's time is extremely valuable, marshalling
 * the reindeer or elves into a group must not be done by Santa.
 *
 * SOLUTION:
 *
 * The following sloution is based on stdlib Scala Actors: one actor per each
 * Santa's helpers (elves and reindder), one actor for Santa, and one more actor
 * for Santa's secretary that assembles groups of helpers, and sends them to
 * Santa
 * 
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
