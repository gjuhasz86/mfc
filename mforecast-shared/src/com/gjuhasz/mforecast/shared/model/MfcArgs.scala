package com.gjuhasz.mforecast.shared.model

import java.time.LocalDate

case class MfcArgs(
  start: LocalDate,
  cashflows: List[Cashflow],
  defaultAccount: Account,
  allocated: Map[Category, Int]
)
