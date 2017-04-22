package com.gjuhasz.mforecast.shared.model

import java.time.LocalDate

//trait Category {def name: String = this.toString }
//object Category {
//  def apply(name0: String) = new Category {override def name = name0 }
//}


case class Category(name: String)
case class Account(name: String)

sealed trait Cashflow {
  def date: LocalDate
  def amount: Int
}
case class Spending(date: LocalDate, category: Category, amount: Int) extends Cashflow
case class Earning(date: LocalDate, account: Account, amount: Int) extends Cashflow

object Cashflow {
  val earning: PartialFunction[Cashflow, Earning] = { case e@Earning(_, _, _) => e }
  val spending: PartialFunction[Cashflow, Spending] = { case s@Spending(_, _, _) => s }
}

case class Allocation(
  allocated: LocalDate,
  expiry: LocalDate,
  account: Account,
  category: Category,
  amount: Int)
case class FreeAllocation(date: LocalDate, account: Account, amount: Int)

