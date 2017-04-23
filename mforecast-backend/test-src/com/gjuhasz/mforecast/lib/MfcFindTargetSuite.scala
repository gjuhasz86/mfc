package com.gjuhasz.mforecast.lib

import com.gjuhasz.mforecast.lib.Mfc._
import org.scalatest.{FlatSpec, Matchers}

class MfcFindTargetSuite extends FlatSpec with Matchers {

  "Target" should "be empty if the input list is empty" in {
    findTarget(Nil, 0) shouldBe Nil
  }

  it should "throw when amount is negative" in {
    val thrown = the[IllegalArgumentException] thrownBy findTarget(List((1, 1)), -1)
    thrown.getMessage shouldBe "requirement failed: Negative amount is not supported [-1]"
  }

  it should "throw when input list is invalid" in {
    val thrown = the[IllegalArgumentException] thrownBy findTarget(List((0, 0), (0, -6)), 0)
    thrown.getMessage shouldBe "requirement failed: Negative allocation is not supported [List(0, -6)]"
  }

  it should "be zero if the amount is zero" in {
    findTarget(List((0, 0)), 0) shouldBe List(0)
  }

  it should "be zero for all items if the amount is zero" in {
    findTarget(List((0, 0), (0, 0)), 0) shouldBe List(0, 0)
  }


  it should "be the same as the amount if the list has one zero" in {
    findTarget(List((0, 0)), 1) shouldBe List(1)
  }

  it should "be the sum of the item and the amount if the list has one item" in {
    findTarget(List((1, 1)), 1) shouldBe List(2)
  }

  it should "be the average of the sum of the list and the amount evenly distributed" in {
    findTarget(List((1, 1), (1, 1)), 10) shouldBe List(6, 6)
  }

  it should "be the average of the sum of the list and the amount evenly distributed even if the input is uneven" in {
    findTarget(List((1, 1), (1, 2)), 9) shouldBe List(6, 6)
  }

  it should "be the average of the sum of the list and the amount evenly distributed even if the input has zeros" in {
    findTarget(List((1, 1), (1, 2), (1, 0), (1, 0)), 5) shouldBe List(2, 2, 2, 2)
  }

  it should "be the average of the sum of the list and the amount as evenly distributed as possible" in {
    findTarget(List((1, 1), (1, 2), (1, 0), (1, 0)), 8) shouldBe List(3, 3, 3, 2)
  }

  it should "ignore the items higher than the target" in {
    findTarget(List((1, 2), (1, 8)), 4) shouldBe List(6, 8)
  }

  it should "ignore the items higher than the average #3" in {
    findTarget(List((2, 4), (1, 8), (0, 0), (0, 0)), 4) shouldBe List(8, 8, 0, 0)
  }
}
