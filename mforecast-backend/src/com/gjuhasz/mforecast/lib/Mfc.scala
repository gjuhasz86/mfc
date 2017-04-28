package com.gjuhasz.mforecast.lib

import java.time.LocalDate

import com.gjuhasz.mforecast.shared.lib.Utils._
import com.gjuhasz.mforecast.shared.model._
import com.typesafe.scalalogging.LazyLogging

import scala.annotation.tailrec
import scala.collection.immutable.Nil
import scala.math.Ordering.Implicits._
import scala.math.min

object Mfc extends LazyLogging {

  def withDefaultAccount(acc: Account) = new {
    def plan(
      date: LocalDate,
      cashflows: List[Cashflow],
      allocated: Map[Category, Int] = Map().withDefaultValue(0)
    ): List[Allocation] =
      Mfc.plan(date, cashflows, acc, allocated)
  }


  def plan(
    date: LocalDate,
    cashflows: List[Cashflow],
    defaultAccount: Account,
    allocated: Map[Category, Int]
  ): List[Allocation] = {

    val ers = cashflows
      .collect(Cashflow.earning)
      .map(e => (e, e.amount))

    val sps = cashflows
      .collect(Cashflow.spending)
      .groupBy(_.category)
      .toList
      .sortBy(_._2.headOption.map(_.date))

    plan(date, ers, sps, defaultAccount, allocated)
      .filter(_.amount != 0)
  }

  def plan(
    date: LocalDate,
    earnings: List[(Earning, Int)],
    spendings: List[(Category, List[Spending])],
    defaultAccount: Account,
    allocated: Map[Category, Int]
  ): List[Allocation] =
    spendings match {
      case Nil => Nil
      case (category, currSpendings) :: spendingTail =>
        logger.info(s"Categories left: [${ spendings.size }]")
        val (initial, allocations) =
          plan(date, earnings, currSpendings, allocated(category), defaultAccount)

        assert(allocations.size % earnings.size == 0, "Allocations' size should be the multiple of the earnigns' size")

        // the n-th allocation corresponds to the (n mod earnings.size)-th earning
        val allocationMatrix = allocations.grouped(earnings.size).toList.transpose
        val newEarnings = (earnings zip allocationMatrix).map {
          case ((earning, unallocated), allocs) =>
            (earning, unallocated - allocs.map(_.amount).sum)
        }

        initial ::: allocations ::: plan(date, newEarnings, spendingTail, defaultAccount, allocated)
    }

  // plans for a single category
  // guarantees that the size of the result will be a multiple of the size of the earnings
  def plan(
    date: LocalDate,
    earnings: List[(Earning, Int)],
    spendings: List[Spending],
    allocated: Int,
    defaultAcc: Account
  ): (List[Allocation], List[Allocation]) =
  spendings match {
    case Nil => (Nil, Nil)
    case spending :: spendingsTail =>
      logger.info(s"Spending left in category: [${ spendings.size }]")
      val earningItems =
        earnings
          .map {
            case (eng, unall) if eng.date > spending.date.prevMonthEnd =>
              EarningItem(eng.amount, 0)
            case (eng, unall) =>
              EarningItem(eng.amount, unall)
          }
      val allocationRes = allocate(earningItems, allocated, spending.amount)

      val initAlloc =
        Allocation(
          allocated = date,
          expiry = spending.date.prevMonthEnd,
          account = defaultAcc,
          category = spending.category,
          amount = allocationRes.initial)

      val allocations = (earnings zip allocationRes.allocations).map {
        case ((earning, unallocated), amount) =>
          Allocation(
            allocated = earning.date,
            expiry = spending.date.prevMonthEnd,
            account = earning.account,
            category = spending.category,
            amount = amount)

      }

      val newEarnings = (earnings zip allocationRes.allocations).map {
        case ((earning, unallocated), amount) =>
          (earning, unallocated - amount)
      }
      val newAllocated = min(allocated - allocationRes.initial, 0)

      val currentRes = initAlloc :: allocations
      val (initTailAlloc, tailAllocations) =
        plan(date, newEarnings, spendingsTail, newAllocated, defaultAcc)

      (initAlloc :: initTailAlloc, allocations ::: tailAllocations)
  }


  case class AllocationResult(initial: Int, allocations: List[Int])

  case class EarningItem(full: Int, unallocated: Int) {
    def asTuple: (Int, Int) = (full, unallocated)
    def allocated: Int = full - unallocated
  }
  object EarningItem {
    def fromAu(allocated: Int, unallocated: Int) = EarningItem(allocated + unallocated, unallocated)
  }


  def allocate(earnings: List[EarningItem], allocated: Int, spending: Int): AllocationResult = {
    if (allocated >= spending) {
      AllocationResult(spending, Nil)
    } else {
      val res = allocate(earnings, spending - allocated)
      res.copy(initial = res.initial + allocated)
    }
  }

  def allocate(earnings: List[EarningItem], spending: Int): AllocationResult = {
    val (full, unallocated) = earnings.map(_.asTuple).unzip
    val allocated = earnings.map(_.allocated)

    val targetAllocations =
      (allocated zip findTarget(full zip allocated, spending)).map {
        case (allocd, target) => if (target > allocd) target - allocd else 0
      }

    val allocations =
      (unallocated zip targetAllocations)
        .map { case (u, a) => min(u, a) }


    val initial = spending - allocations.sum
    AllocationResult(initial, allocations)
  }


  def findTarget(input: List[(Int, Int)], amount: Int): List[Int] = {
    require(amount >= 0, s"Negative amount is not supported [$amount]")
    val (full0, allocated0) = input.unzip
    require(allocated0.forall(_ >= 0), s"Negative allocation is not supported [$allocated0]")

    @tailrec
    def loop(ignore: List[Boolean]): List[Int] = {
      val (full, allocated) = (ignore zip input)
        .map { case (i, x) => if (i) (0, 0) else x }
        .unzip

      val sum = allocated.sum + amount
//      require(sum <= Int.MaxValue, s"Amount is too high [$sum]")

      val targets = distribute(full, sum.toInt)
      val tempTargets = allocated zip targets

      val nextIgnore = (ignore zip tempTargets)
        .map { case (ig, (allocd, target)) => ig || allocd > target }

      if (nextIgnore == ignore) {
        (ignore zip (allocated0 zip targets))
          .map { case (i, (a, t)) => if (i) a else Math.max(a, t) }
      } else {
        loop(nextIgnore)
      }
    }

    loop(input.map(_ => false))
  }

  def distribute(list: List[Int], n: Int): List[Int] = {
    def loop(acc: List[Int], list: List[Int], n: Int): List[Int] = list match {
      case Nil => acc
      case lst if lst.sum == 0 => loop(acc, lst.map(_ => 1), n)
      case head :: tail =>
        val h = Math.ceil(n * (head.toDouble / list.map(_.toLong).sum)).toInt
        loop(h :: acc, tail, n - h)
    }
    loop(Nil, list, n).reverse
  }


}