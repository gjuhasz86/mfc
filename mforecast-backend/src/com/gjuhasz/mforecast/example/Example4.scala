package com.gjuhasz.mforecast.example

import com.gjuhasz.mforecast.lib.Mfc
import com.gjuhasz.mforecast.shared.lib.Syntax.Dsl
import com.gjuhasz.mforecast.shared.lib.Utils._
import com.gjuhasz.mforecast.shared.model._

object Example4 {
  private val start = "2017-04-23".d
  private val forecastPeriod = 10.years

  def main(args: Array[String]): Unit = {
    val dsl = Dsl(start, forecastPeriod)
    import dsl._

    val spendings = List(
      spend { 100 } on { Car } due_in { 1.months } and_every { 1.month },
      spend { 200 } on { Food } due_in { 1.months } and_every { 1.month }
    ).flatten

    val earnings = List(
      earn { 300 } on { Combined } due_in { 0.day } and_every { 1.month }
    ).flatten

    val cashflows = (earnings ::: spendings).sortBy(_.date)

    val res = Mfc.plan(start, cashflows, Current, Map().withDefaultValue(0))
      .sortBy(_.allocated)
    res map println
  }

  object Rent extends Category("Rent")
  object Food extends Category("Food")
  object Cafe extends Category("Cafe")
  object Travel extends Category("Travel")
  object Car extends Category("Car")
  object Current extends Account("Current")
  object Combined extends Account("Combined")
}
