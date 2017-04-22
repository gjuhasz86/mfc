package com.gjuhasz.mforecast.client

import java.nio.ByteBuffer
import java.time.LocalDate

import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.response.SimpleHttpResponse
import monix.execution.Scheduler.Implicits.global

import scala.scalajs.js.annotation.{JSExport, JSExportAll, JSExportTopLevel}
import scala.util.{Failure, Success, Try}
import com.gjuhasz.mforecast.shared.lib.Syntax.Dsl
import com.gjuhasz.mforecast.shared.lib.Utils._
import com.gjuhasz.mforecast.shared.utils.JsonBody._
import example.shared.Foo
import fr.hmil.roshttp.body.BulkBodyPart
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import com.gjuhasz.mforecast.shared.utils.CirceInstances._
import com.gjuhasz.mforecast.shared.model.mjs.MjsImplicits._

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import com.gjuhasz.mforecast.shared.model._
import com.gjuhasz.mforecast.shared.model.mjs.{CashflowSpec, PeriodUnit}

@JSExportTopLevel("Mfc")
@JSExportAll
object MfcClient {

  def createRequest(): String = {

    object Rent extends Category("Rent")
    object Food extends Category("Food")
    object Cafe extends Category("Cafe")
    object Travel extends Category("Travel")
    object Car extends Category("Car")

    object Current extends Account("Current")
    object Combined extends Account("Combined")

    val start = "2017-01-01".d
    val forecastPeriod = 10.years

    val dsl = Dsl(start, forecastPeriod)
    import dsl._

    val spendings = List(
      spend { 60000 } on { Travel } once() on { "2017-04-10".d },
      spend { 40000 } on { Rent } once() on { "2017-05-10".d },
      spend { 2600000 } on { Car } once() on { "2017-07-10".d }
    ).flatten

    val earnings = List(
      earn { 100000 } on { Combined } once() on { "2017-01-02".d },
      earn { 200000 } on { Combined } once() on { "2017-02-01".d },
//      earn { 100000 } on { Combined } once() on { "2017-02-02".d },
      earn { 100000 } on { Combined } once() on { "2017-03-01".d },
      earn { 100000 } on { Combined } once() on { "2017-04-01".d },
      earn { 100000 } on { Combined } once() on { "2017-05-01".d },
      earn { 100000 } on { Combined } once() on { "2017-06-01".d },
      earn { 100000 } on { Combined } due_in { 1.months } and_every { 1.months },
      earn { 100000 } on { Combined } once() on { "2017-07-01".d }
    ).flatten

    val cashflows = (earnings ::: spendings).sortBy(_.date)

    val mfcArgs = MfcArgs(start, cashflows, Combined, Map().withDefaultValue(0))
    mfcArgs.asJson.noSpaces
  }

  private val CfRe = """([se]) +([0-9]+) +on +([^ ]+)( in ([0-9]+) *([^ ]+))? +x +([0-9]+) *?([^ ]+)""".r
  def parseCashflow(cfStr: String): js.UndefOr[CashflowSpec] = {
    cfStr match {
      case CfRe(sr, am, cat, _, due, dueUnit, per, perUnit) =>
        CashflowSpec.create(sr, am, cat, due, dueUnit, per, perUnit).orUndefined
      case x =>
        js.undefined
    }
  }

  private val DateRe = """([0-9]{4})-([0-9]{2})-([0-9]{2})""".r
  def validateLocalDate(str: String): Boolean = str match {
    case DateRe(y, m, d) => Try {
      LocalDate.of(y.toInt, m.toInt, d.toInt)
    }.isSuccess
    case _ => false
  }

  private val PeriodRe = """([0-9]+) *?([^ ]+)""".r
  def validatePeriod(str: String): Boolean = str match {
    case PeriodRe(v, u) => Try { v.toInt }.isSuccess && PeriodUnit.unapply(u).isDefined
    case _ => false
  }

  def parsePeriod(str: String) = str match {
    case PeriodRe(v, PeriodUnit(u)) => u.short match {
      case "d" => v.toInt.days
      case "w" => v.toInt.weeks
      case "m" => v.toInt.months
      case "y" => v.toInt.years
    }
  }

  def parseResponse(resp: String): js.Array[mjs.Allocation] =
    decode[List[Allocation]](resp)
      .getOrElse(Nil)
      .map(_.toJs)
      .toJSArray

  def rollout(c: CashflowSpec, startStr: String, periodStr: String): js.Array[Cashflow] = {
    //    val start = "2017-01-01".d
    //    val forecastPeriod = 10.years

    val start = startStr.d
    val forecastPeriod = parsePeriod(periodStr)
    val dsl = Dsl(start, forecastPeriod)
    import dsl._

    val per = c.periodUnit.short match {
      case "d" => c.periodValue.days
      case "w" => c.periodValue.weeks
      case "m" => c.periodValue.months
      case "y" => c.periodValue.years
    }

    val due = (c.dueValue.toOption, c.dueUnit.map(_.short).toOption) match {
      case (Some(d), Some("d")) => d.days
      case (Some(d), Some("w")) => d.weeks
      case (Some(d), Some("m")) => d.months
      case (Some(d), Some("y")) => d.years
      case (None, None) => 0.days
      case (v, u) => throw new IllegalStateException(s"Inconsistent due value and unit: [$v] and [$u]")
    }

    val cashflows: List[Cashflow] = c.verb match {
      case "earn" =>
        earn { c.amount } on { Account(c.catOrAcc) } due_in { due } and_every { per }
      case "spend" =>
        spend { c.amount } on { Category(c.catOrAcc) } due_in { due } and_every { per }
    }
    cashflows foreach println
    cashflows.toJSArray
  }

  def genChartInput(as: js.Array[mjs.Allocation], cfs: js.Array[Cashflow]): js.Array[js.Array[Any]] = {
    val cats = as.map(_.category.name).distinct
    val months = cfs.collect { case e: Earning => e }.map(_.date.withDayOfMonth(1)).distinct.sorted
//    val months = as.map(_.allocated).distinct.sorted

    val res = months.map { m =>
      val alloc = cats.map { c =>
        as.filter(_.allocated.withDayOfMonth(1) == m).filter(_.category.name == c).map(_.amount).sum
      }
      val spend = cats.map { c =>

        cfs.filter(_.date.withDayOfMonth(1) == m.withDayOfMonth(1))
          .collect { case s: Spending => s }
          .filter(_.category.name == c)
          .map(_.amount)
          .sum
      }

      val earn = cfs.filter(_.date.withDayOfMonth(1) == m.withDayOfMonth(1))
        .collect { case e: Earning => e }
        .map(_.amount)
        .sum

      val unalloc = earn - alloc.sum

      m.toString +: alloc :+ unalloc  //(alloc ++ spend)
    }

    val header: js.Array[Any] = "Categories" +: cats :+ "Unallocated" //(cats ++ cats)

    header +: res
  }

  def genC3ChartInput(as: js.Array[mjs.Allocation], cfs: js.Array[Cashflow]): js.Array[Any] = {
    val cats = as.map(_.category.name).distinct
    val months = cfs.collect { case e: Earning => e }.map(_.date.withDayOfMonth(1)).distinct.sorted
//    val months = as.map(_.allocated).distinct.sorted

    val allSeries: js.Array[js.Array[Any]] = cats.map { c =>
      val series = months.map { m =>
        as.filter(_.allocated.withDayOfMonth(1) == m).filter(_.category.name == c).map(_.amount).sum
      }
      (c: Any) +: series
    }

//    val res: js.Array[js.Array[Any]] = months.map { m =>
//      val alloc = cats.map { c =>
//        as.filter(_.allocated.withDayOfMonth(1) == m).filter(_.category.name == c).map(_.amount).sum
//      }
//      val spend = cats.map { c =>
//
//        cfs.filter(_.date.withDayOfMonth(1) == m.withDayOfMonth(1))
//          .collect { case s: Spending => s }
//          .filter(_.category.name == c)
//          .map(_.amount)
//          .sum
//      }
//
//      val earn = cfs.filter(_.date.withDayOfMonth(1) == m.withDayOfMonth(1))
//        .collect { case e: Earning => e }
//        .map(_.amount)
//        .sum
//
//      val unalloc = earn - alloc.sum
//
//      m.toString +: alloc :+ unalloc  //(alloc ++ spend)
//    }
//
//    val header: js.Array[Any] = "Categories" +: cats :+ "Unallocated" //(cats ++ cats)

    js.Array(months.map(_.toString), allSeries)
  }

  def genAmChartInput(as: js.Array[mjs.Allocation], cfs: js.Array[Cashflow]): js.Array[Any] = {
    val cats = as.map(_.category.name).distinct
    val months = cfs.collect { case e: Earning => e }.map(_.date.withDayOfMonth(1)).distinct.sorted
//    val months = as.map(_.allocated).distinct.sorted

    val res = months.map { m =>
      val alloc = cats.map { c =>
        as.filter(_.allocated.withDayOfMonth(1) == m).filter(_.category.name == c).map(_.amount).sum
      }
      val spend = cats.map { c =>

        cfs.filter(_.date.withDayOfMonth(1) == m.withDayOfMonth(1))
          .collect { case s: Spending => s }
          .filter(_.category.name == c)
          .map(_.amount)
          .sum
      }

      val earn = cfs.filter(_.date.withDayOfMonth(1) == m.withDayOfMonth(1))
        .collect { case e: Earning => e }
        .map(_.amount)
        .sum

      val unalloc = earn - alloc.sum

      val zipped = (cats :+ "Unallocated") zip (alloc :+ unalloc)
      (zipped.toMap + ("category" -> m.toString)).toJSDictionary: Any
    }
    js.Array(cats :+ "Unallocated", res)
  }

  def makeRequest(cashflows: js.Array[Cashflow]): String = {
    val start = "2017-01-01".d
    val forecastPeriod = 10.years
    val defaultAccount = Account("Default")
    val mfcArgs = MfcArgs(start, cashflows.toList, defaultAccount, Map())
    mfcArgs.asJson.noSpaces
  }

}
