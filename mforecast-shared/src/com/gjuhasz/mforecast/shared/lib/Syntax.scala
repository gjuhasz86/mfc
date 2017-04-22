package com.gjuhasz.mforecast.shared.lib

import java.time.{LocalDate, Period}

import com.gjuhasz.mforecast.shared.lib.Schedule.Syntax._
import com.gjuhasz.mforecast.shared.lib.Utils._
import com.gjuhasz.mforecast.shared.model._

object Syntax {

  case class Earning0(acc: Account, amount: Int)
  case class Spending0(cat: Category, amount: Int)
  case class Allocation0(cat: Category, amount: Int)

  private val toSpending = (date: LocalDate, spending0: Spending0) =>
    Spending(date, spending0.cat, spending0.amount)
  private val toEarning = (date: LocalDate, earning0: Earning0) =>
    Earning(date, earning0.acc, earning0.amount)

  case class Dsl(private val start: LocalDate, private val forecastPeriod: Period) {
    def spend(amount: Int) = new {
      def on(category: Category) = new {
        def once() = new {
          def on(date: LocalDate) =
            List(Spending(date, category, amount))
          def in(period: Period) =
            List(Spending(start.plus(period), category, amount))
        }
        def monthly =
          (REPEAT { Spending0(category, amount) } EVERY { month } FOR { forecastPeriod } START_ON { start } ROLL OUT)
            .map { toSpending.tupled }
        def due_in(startPeriod: Period) = new {
          def and_every(period: Period) =
            (REPEAT { Spending0(category, amount) } EVERY { period } FOR { forecastPeriod } START_ON { start.plus(startPeriod) } ROLL OUT)
              .map { toSpending.tupled }
        }
      }
    }

    def earn(amount: Int) = new {
      def on(account: Account) = new {
        def once() = new {
          def on(date: LocalDate) =
            List(Earning(date, account, amount))
        }
        def monthly =
          (REPEAT { Earning0(account, amount) } EVERY { month } FOR { forecastPeriod } START_ON { start } ROLL OUT)
            .map { toEarning.tupled }
        def due_in(startPeriod: Period) = new {
          def and_every(period: Period) =
            (REPEAT { Earning0(account, amount) } EVERY { period } FOR { forecastPeriod } START_ON { start.plus(startPeriod) } ROLL OUT)
              .map { toEarning.tupled }
        }
      }
    }
  }

}