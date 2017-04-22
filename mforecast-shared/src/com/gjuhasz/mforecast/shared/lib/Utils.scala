package com.gjuhasz.mforecast.shared.lib

import java.time.temporal.TemporalAmount
import java.time.{LocalDate, Period, YearMonth}

object Utils {
  implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

  implicit class StringToDate(val self: String) extends AnyVal {
    def d: LocalDate =
      LocalDate.of(
        self.substring(0, 4).toInt,
        self.substring(5, 7).toInt,
        self.substring(8, 10).toInt)
  }

  implicit class RichLocalDate(val self: LocalDate) extends AnyVal {
    def prevMonthEnd: LocalDate = YearMonth.from(self).minusMonths(1).atEndOfMonth
    def +(temporalAmount: TemporalAmount): LocalDate = self.plus(temporalAmount)
    def ym: YearMonth = YearMonth.from(self)
  }

  implicit class RichPeriod(self: Period) {
    def *(scalar: Int): Period = self.multipliedBy(scalar)
  }

  implicit class SafeOption[T](self: Some[T]) {
    def safeGet: T = self.get
  }

  implicit class IntToPeriod(self: Int) {
    def day: Period = Period.ofDays(self)
    def days: Period = Period.ofDays(self)
    def week: Period = Period.ofWeeks(self)
    def weeks: Period = Period.ofWeeks(self)
    def month: Period = Period.ofMonths(self)
    def months: Period = Period.ofMonths(self)
    def year: Period = Period.ofYears(self)
    def years: Period = Period.ofYears(self)
  }

  val day = 1.day
  val week = 1.week
  val month = 1.month
  val year = 1.year
}
