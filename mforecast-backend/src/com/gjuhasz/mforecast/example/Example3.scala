package com.gjuhasz.mforecast.example

import com.gjuhasz.mforecast.lib.Mfc
import com.gjuhasz.mforecast.shared.lib.Syntax.Dsl
import com.gjuhasz.mforecast.shared.lib.Utils._
import com.gjuhasz.mforecast.shared.model._

object Example3 {
  private val start = "2017-02-10".d
  private val forecastPeriod = 2.months

  def main(args: Array[String]): Unit = {
    val dsl = Dsl(start, forecastPeriod)
    import dsl._

    val spendings = List(
      spend { 50 } on { Food } once() on { "2017-04-10".d },
      spend { 140 } on { Rent } once() on { "2017-03-10".d }
    ).flatten

    val earnings = List(
      earn { 100 } on { Combined } once() on { "2017-02-11".d },
      earn { 100 } on { Current } once() on { "2017-02-11".d },
      earn { 100 } on { Combined } once() on { "2017-03-11".d },
      earn { 100 } on { Current } once() on { "2017-03-11".d }
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
