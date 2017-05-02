package com.gjuhasz.mforecast.shared.model.mjs

import java.time.LocalDate

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.{JSGlobal, ScalaJSDefined}
import scala.util.Try

object MjsImplicits {
  implicit class AllocationConverter(self: com.gjuhasz.mforecast.shared.model.Allocation) {
    def toJs: Allocation = new Allocation(
      self.allocated,
      self.expiry,
      new Account(self.account.name),
      new Category(self.category.name),
      self.amount
    )
  }
}

@ScalaJSDefined
class Category(val name: String) extends js.Object {
  override def equals(o: scala.Any): Boolean = o match {
    case c: Category => c.name == name
    case _ => false
  }

  override def hashCode(): Int = name.hashCode
  override def toString: String = s"Category($name)"
}


@ScalaJSDefined
class Account(val name: String) extends js.Object {
  override def equals(o: scala.Any): Boolean = o match {
    case c: Account => c.name == name
    case _ => false
  }

  override def hashCode(): Int = name.hashCode
  override def toString: String = s"Account($name)"
}

@ScalaJSDefined
class Allocation(
  val allocated: LocalDate,
  val expiry: LocalDate,
  val account: Account,
  val category: Category,
  val amount: Int) extends js.Object


@ScalaJSDefined
sealed trait PeriodUnit extends js.Object {
  def short: String
  def singular: String
  def plural: String
}
object PeriodUnit {
  def unapply(s: String): Option[PeriodUnit] = s match {
    case Day(x) => Some(x)
    case Week(x) => Some(x)
    case Month(x) => Some(x)
    case Year(x) => Some(x)
    case _ => None
  }
}

@ScalaJSDefined
object Day extends PeriodUnit {
  override def short: String = "d"
  override def singular: String = "day"
  override def plural: String = "days"
  def validate(s: String): Boolean = List(short, singular, plural) contains s.toLowerCase
  def unapply(s: String): Option[Day.type] = if (validate(s)) Some(Day) else None
}
@ScalaJSDefined
object Week extends PeriodUnit {
  override def short: String = "w"
  override def singular: String = "week"
  override def plural: String = "weeks"
  def validate(s: String): Boolean = List(short, singular, plural) contains s.toLowerCase
  def unapply(s: String): Option[Week.type] = if (validate(s)) Some(Week) else None
}
@ScalaJSDefined
object Month extends PeriodUnit {
  override def short: String = "m"
  override def singular: String = "month"
  override def plural: String = "months"
  def validate(s: String): Boolean = List(short, singular, plural) contains s.toLowerCase
  def unapply(s: String): Option[Month.type] = if (validate(s)) Some(Month) else None
}
@ScalaJSDefined
object Year extends PeriodUnit {
  override def short: String = "y"
  override def singular: String = "year"
  override def plural: String = "years"
  def validate(s: String): Boolean = List(short, singular, plural) contains s.toLowerCase
  def unapply(s: String): Option[Year.type] = if (validate(s)) Some(Year) else None
}

@ScalaJSDefined
sealed trait CashflowSpec extends js.Object {
  def verb: String
  def amount: Int
  def catOrAcc: String
  def dueValue: js.UndefOr[Int]
  def dueUnit: js.UndefOr[PeriodUnit]
  def periodValue: js.UndefOr[Int]
  def periodUnit: js.UndefOr[PeriodUnit]
  def lenValue: js.UndefOr[Int]
  def lenUnit: js.UndefOr[PeriodUnit]
}


object CashflowSpec {

  def createTyped(
    verb: String, amount: Int, catOrAcc: String,
    dueValue: Option[Int], dueUnit: Option[PeriodUnit],
    periodValue: Option[Int], periodUnit: Option[PeriodUnit],
    lenValue: Option[Int], lenUnit: Option[PeriodUnit]
  ): Option[CashflowSpec] = verb match {
    case "e" | "earn" =>
      Some(new IncomeSpec(amount, catOrAcc, dueValue.orUndefined, dueUnit.orUndefined, periodValue.orUndefined, periodUnit.orUndefined, lenValue.orUndefined, lenUnit.orUndefined))
    case "s" | "spend" =>
      Some(new ExpenseSpec(amount, catOrAcc, dueValue.orUndefined, dueUnit.orUndefined, periodValue.orUndefined, periodUnit.orUndefined, lenValue.orUndefined, lenUnit.orUndefined))
    case _ => None
  }

  def create(
    verb: String, amount: String, catOrAcc: String,
    due: String, dueUnitStr: String,
    per: String, perUnitStr: String,
    len: String, lenUnitStr: String
  ): Option[CashflowSpec] = {
    val duOpt = Option(dueUnitStr) match {
      case Some(du) => PeriodUnit.unapply(du).map(Some(_))
      case None => Some(None)
    }

    val puOpt = Option(perUnitStr) match {
      case Some(pu) => PeriodUnit.unapply(pu).map(Some(_))
      case None => Some(None)
    }

    val luOpt = Option(lenUnitStr) match {
      case Some(lu) => PeriodUnit.unapply(lu).map(Some(_))
      case None => Some(None)
    }

    for {
      am <- Try(amount.toInt).toOption
      d <- Try(Option(due).map(_.toInt)).toOption
      du <- duOpt
      p <- Try(Option(per).map(_.toInt)).toOption
      pu <- puOpt
      l <- Try(Option(len).map(_.toInt)).toOption
      lu <- luOpt
      res <- createTyped(verb, am, catOrAcc, d, du, p, pu, l, lu)
    } yield res
  }

}

@ScalaJSDefined
class IncomeSpec(
  override val amount: Int,
  override val catOrAcc: String,
  override val dueValue: UndefOr[Int],
  override val dueUnit: UndefOr[PeriodUnit],
  override val periodValue: UndefOr[Int],
  override val periodUnit: UndefOr[PeriodUnit],
  override val lenValue: UndefOr[Int],
  override val lenUnit: UndefOr[PeriodUnit]) extends CashflowSpec {
  override def verb: String = "earn"
}

@ScalaJSDefined
class ExpenseSpec(
  override val amount: Int,
  override val catOrAcc: String,
  override val dueValue: UndefOr[Int],
  override val dueUnit: UndefOr[PeriodUnit],
  override val periodValue: UndefOr[Int],
  override val periodUnit: UndefOr[PeriodUnit],
  override val lenValue: UndefOr[Int],
  override val lenUnit: UndefOr[PeriodUnit]) extends CashflowSpec {
  override def verb: String = "spend"
}
