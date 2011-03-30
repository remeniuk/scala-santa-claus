/*
 * THE SANTA CLAUS PROBLEM:
 *
 * Santa Claus sleeps at the North pole until awakened by either all
 * of the nine reindeer, or by a group of three out of ten elves. He
 * performs one of two indivisible actions:
 *
 ²       - If awakened by the group of reindeer, Santa harnesses them to
 *       a sleigh, delivers toys, and finally unharnesses the reindeer who
 *       then go on vacation.
 ²       - If awakened by a group of elves, Santa shows them into his
 *       office, consults with them on toy R&D, and finally shows them
 *       out so they can return to work constructing toys.
 *
 * A waiting group of reindeer must be served by Santa before a waiting
 * group of elves. Since Santa’s time is extremely valuable, marshalling
 * the reindeer or elves into a group must not be done by Santa.
 *
 * SOLUTION:
 *
 * The following sloution is based on Scala STM and stdlib Actors, and is very
 * much influenced by the solution provided in "Beautiful Code" by Simon Peyton
 * Jones (the only noticeable difference is that actors are used instead of
 * thread as in Haskell)
 */

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