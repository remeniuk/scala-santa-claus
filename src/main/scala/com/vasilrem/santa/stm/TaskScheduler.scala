package com.vasilrem.santa.stm

import java.util.concurrent.{Executors, TimeUnit}

object TaskScheduler {
  private lazy val sched = Executors.newSingleThreadScheduledExecutor

  def schedule(f: => Unit, time: Long) =
    sched.schedule(new Runnable{def run = actors.Scheduler.execute(f)},
                   time, TimeUnit.MILLISECONDS)

}

