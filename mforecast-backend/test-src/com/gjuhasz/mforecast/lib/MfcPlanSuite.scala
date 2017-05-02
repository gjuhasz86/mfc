package com.gjuhasz.mforecast.lib

import com.gjuhasz.mforecast.shared.lib.Syntax.Dsl
import com.gjuhasz.mforecast.shared.lib.Utils._
import com.gjuhasz.mforecast.shared.model._
import org.scalatest.FunSuite

class MfcPlanSuite extends FunSuite {

  val mfc = Mfc.withDefaultAccount(Current)

  test("No spendings - no allocations") {
    assert(mfc.plan(jan01, Nil) == Nil)
  }

  test("Single earning & spending - single allocation") {
    val cashflows = List(
      Earning(jan11, Current, 1000),
      Spending(feb10, Rent, 100)
    )

    val expected = List(
      Allocation(jan11, jan31, Current, Rent, 100)
    )

    assert(mfc.plan(jan01, cashflows) == expected)
  }

  test("Two earnings, one spending - two allocations") {
    val cashflows = List(
      Earning(jan01, Current, 1000),
      Earning(feb10, Current, 1000),
      Spending(mar02, Rent, 100)
    )

    val expected = List(
      Allocation(jan01, feb28, Current, Rent, 50),
      Allocation(feb10, feb28, Current, Rent, 50)
    )

    assert(mfc.plan(jan01, cashflows) == expected)
  }

  test("Two earnings, one spending with prime amount - two allocations that add up to the amount") {
    val cashflows = List(
      Earning(jan01, Current, 1000),
      Earning(feb10, Current, 1000),
      Spending(mar02, Rent, 97)
    )

    val expected = List(
      Allocation(jan01, feb28, Current, Rent, 49),
      Allocation(feb10, feb28, Current, Rent, 48)
    )

    assert(mfc.plan(jan01, cashflows) == expected)
  }


  test("It should work when categories are duplicated") {
    val cashflows = List(
      Earning(jan01, Current, 1000),
      Earning(feb10, Current, 1000),
      Spending(feb12, Rent, 20),
      Spending(mar02, Rent, 20),
      Spending(apr12, Food, 30)
    )

    val expected = List(
      Allocation(jan01, jan31, Current, Rent, 20),
      Allocation(jan01, mar31, Current, Food, 15),
      Allocation(feb10, feb28, Current, Rent, 20),
      Allocation(feb10, mar31, Current, Food, 15)
    )

    val actual = mfc.plan(jan01, cashflows)
      .sortBy(a => (a.allocated, a.expiry, a.account.name, a.category.name))
    assert(actual == expected)
  }

  test("It should work when categories are duplicated and the first earning is in the future") {
    val cashflows = List(
      Earning(feb10, Current, 150),
      Earning(mar02, Current, 150),
      Earning(apr12, Current, 150),
      Spending(feb10, Rent, 100),
      Spending(mar02, Rent, 100),
      Spending(apr12, Rent, 100),
      Spending(may15, Rent, 100)
    )

    val expected = List(
      Allocation(jan11, jan31, Current, Rent, 100),
      Allocation(feb10, feb28, Current, Rent, 100),
      Allocation(mar02, mar31, Current, Rent, 100),
      Allocation(apr12, apr30, Current, Rent, 100)
    )

    val actual = mfc.plan(jan11, cashflows)
      .sortBy(a => (a.allocated, a.expiry, a.account.name, a.category.name))
    assert(actual == expected)
  }

  test("It should work when earnings fall on the same date") {
    val cashflows = List(
      Earning(feb10, Current, 100),
      Earning(feb10, Combined, 100),
      Spending(mar02, Rent, 140),
      Spending(apr12, Food, 60)
    )

    val expected = List(
      Allocation(feb10, feb28, Current, Rent, 70),
      Allocation(feb10, feb28, Combined, Rent, 70),
      Allocation(feb10, mar31, Current, Food, 30),
      Allocation(feb10, mar31, Combined, Food, 30)
    )

    assert(mfc.plan(jan01, cashflows) == expected)
  }

  test("It should work when earnings fall on the same date and the same account") {
    val cashflows = List(
      Earning(feb10, Current, 100),
      Earning(feb10, Current, 100),
      Spending(mar02, Rent, 140),
      Spending(apr12, Food, 60)
    )

    val expected = List(
      Allocation(feb10, feb28, Current, Rent, 70),
      Allocation(feb10, feb28, Current, Rent, 70),
      Allocation(feb10, mar31, Current, Food, 30),
      Allocation(feb10, mar31, Current, Food, 30)
    )

    assert(mfc.plan(jan01, cashflows) == expected)
  }

  test("It should not throw StackOverflowError when the calculation period is long") {
    val start = "2017-01-10".d
    val forecastPeriod = 10.years
    val dsl = Dsl(start, forecastPeriod)
    import dsl._

    val cashflows: List[Cashflow] = List(
      spend { 100 } on { Rent } due_in { 1.months } and_every { 1.month },
      spend { 200 } on { Food } due_in { 1.months } and_every { 1.month },
      earn { 300 } on { Combined } due_in { 0.day } and_every { 1.month }
    ).flatten

    Mfc.plan(start, cashflows, Current, Map().withDefaultValue(0))
  }

  private val jan01 = "2017-01-01".d
  private val jan11 = "2017-01-11".d
  private val jan31 = "2017-01-31".d
  private val feb10 = "2017-02-10".d
  private val feb12 = "2017-02-12".d
  private val feb28 = "2017-02-28".d
  private val mar02 = "2017-03-02".d
  private val mar31 = "2017-03-31".d
  private val apr12 = "2017-04-11".d
  private val apr30 = "2017-04-30".d
  private val may15 = "2017-05-15".d

  object Rent extends Category("Rent")
  object Food extends Category("Food")
  object Cafe extends Category("Cafe")

  object Current extends Account("Current")
  object Combined extends Account("Combined")
}
