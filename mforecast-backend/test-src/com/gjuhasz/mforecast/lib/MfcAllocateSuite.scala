package com.gjuhasz.mforecast.lib

import com.gjuhasz.mforecast.lib.Mfc._
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen._
import org.scalatest.prop.{Checkers, PropertyChecks}
import org.scalatest.{FlatSpec, Matchers}

import scala.math.{max, min}

class MfcAllocateSuite extends FlatSpec with Checkers with PropertyChecks with Matchers {

  private val nat = arbitrary[Int] retryUntil (_ >= 0)
  private val natPair = for (a <- nat; b <- nat) yield (a, b)
  private val earnings = listOf[(Int, Int)](natPair)

  private val earningItem = for ((a, b) <- natPair) yield EarningItem.fromAu(a, b)
  private val earningItems = listOf[EarningItem](earningItem)

  private val isNatTup = ((a: Int, b: Int) => a >= 0 && b >= 0).tupled

  "Allocations" should "be the same size as the earnigns" in {
    forAll(earningItems, nat) { case (earnings: List[EarningItem], n: Int) =>
      whenever(n >= 0) {
        allocate(earnings, n).allocations.size shouldBe earnings.size
      }
    }
  }

  they should "add up to the spending" in {
    forAll(earningItems, nat) { case (earnings: List[EarningItem], spending: Int) =>
      whenever(spending >= 0) {
        val res = allocate(earnings, spending)
        res.allocations.sum + res.initial shouldBe spending
      }
    }
  }


  they should "never have negative initial allocation" in {
    forAll(earningItems, nat) { case (earnings: List[EarningItem], n: Int) =>
      whenever(n >= 0) {
        allocate(earnings, n).initial should be >= 0
      }
    }
  }

  "Allocate" should "set the initial allocation when the earning list is empty" in {
    allocate(Nil, 10) shouldBe AllocationResult(10, Nil)
  }

  it should "use the earnings first to cover a spending" in {
    val earnigns = List(
      EarningItem(1000, 1000)
    )
    allocate(earnigns, 100) shouldBe AllocationResult(0, List(100))
  }

  it should "distribute the spending over the earnings" in {
    val earnigns = List(
      EarningItem(1000, 1000),
      EarningItem(1000, 1000)
    )
    allocate(earnigns, 100) shouldBe AllocationResult(0, List(50, 50))
  }

  it should "use the allocated to cover a spending" in {
    allocate(Nil, 500, 100) shouldBe AllocationResult(100, Nil)
  }

  it should "use the allocated before the earning to cover a spending" in {
    val earnigns = List(
      EarningItem(1000, 1000)
    )
    allocate(earnigns, 500, 100) shouldBe AllocationResult(100, Nil)
  }

  it should "use the allocated then the earning to cover a spending" in {
    val earnigns = List(
      EarningItem(1000, 1000)
    )
    allocate(earnigns, 200, 500) shouldBe AllocationResult(200, List(300))
  }

  it should "distribute the spending over the earnings as close as possible" in {
    val earnigns = List(
      EarningItem(1000, 1000),
      EarningItem(1000, 1000)
    )
    allocate(earnigns, 97) shouldBe AllocationResult(0, List(49, 48))
  }

  it should "use initial allocation when the earning is not enough" in {
    val earnigns = List {
      EarningItem(1000, 1000)
    }

    allocate(earnigns, 1500) shouldBe AllocationResult(500, List(1000))
  }

  it should "keep total allocated evenly distributed" in {
    val earnigns = List(
      EarningItem(1000, 900),
      EarningItem(1000, 1000)
    )
    allocate(earnigns, 900) shouldBe AllocationResult(0, List(400, 500))
  }

  it should "keep total allocated evenly distributed when a large allocation is already present" in {
    val earnigns = List(
      EarningItem(1000, 900), // 100, 600,  500, -100, 400
      EarningItem(1000, 200), // 800, 600, -200,    0,   0
      EarningItem(1000, 1000) //   0, 600,  600, -100, 500
    )
    allocate(earnigns, 900) shouldBe AllocationResult(0, List(400, 0, 500))
  }

  it should "keep total allocated evenly distributed when earnings are uneven" in {
    val earnigns = List(
      EarningItem(1000, 900),
      EarningItem(1000, 700),
      EarningItem(2000, 2000)
    )
    allocate(earnigns, 800) shouldBe AllocationResult(0, List(200, 0, 600))
  }

  it should "keep total allocated evenly distributed when multiple large allocation is already present" in {
    val earnigns = List(
      EarningItem(1000, 900), // 100, 475,  375
      EarningItem(1000, 100), // 900,    ,    0
      EarningItem(1000, 450), // 550,    ,    0
      EarningItem(1000, 1000) //   0, 475,  475
    )
    allocate(earnigns, 850) shouldBe AllocationResult(0, List(375, 0, 0, 475))
  }

  it should "use the allocated then the earning then the initial to cover a spending" in {
    val earnigns = List(
      EarningItem(1000, 1000)
    )
    allocate(earnigns, 200, 1500) shouldBe AllocationResult(500, List(1000))
  }

  it should "return a non-negative initial allocation even for large numbers" in {
    val earnings =
      List((10, 0), (2000000000, 0), (1000000000, 0))
        .map((EarningItem.apply _).tupled)

    allocate(earnings, 2147483647).initial should be >= 0
  }

  it should "throw when amount is negative" in {
    val earnings =
      List((1, 1))
        .map((EarningItem.apply _).tupled)

    val thrown = the[IllegalArgumentException] thrownBy allocate(earnings, -1)
    thrown.getMessage shouldBe "requirement failed: Negative amount is not supported [-1]"
  }

//  it should "do something" in {
//    val earnings =
//      List((0,31), (0,2147483647), (0,2147483647), (2,2147483647))
//        .map(EarningItem.tupled)
//
//    val res = allocate(earnings, 0)
//    res.allocations.sum + res.initial shouldBe 0
//  }

}
