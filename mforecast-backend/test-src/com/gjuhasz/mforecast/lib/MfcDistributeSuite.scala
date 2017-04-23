package com.gjuhasz.mforecast.lib

import com.gjuhasz.mforecast.lib.Mfc._
import org.scalatest.{FlatSpec, Matchers}

class MfcDistributeSuite extends FlatSpec with Matchers {

  "Distribute" should "return empty list when the input is empty" in {
    distribute(Nil, 100) shouldBe Nil
  }

  it should "return N if the list has one element" in {
    distribute(List(1), 100) shouldBe List(100)
  }

  it should "return N if the list has one zero element" in {
    distribute(List(0), 100) shouldBe List(100)
  }

  it should "distribute N evenly across the input list" in {
    distribute(List(1, 1), 100) shouldBe List(50, 50)
  }

  it should "distribute N as evenly as possible across two item" in {
    distribute(List(1, 1), 97) shouldBe List(49, 48)
  }

  it should "distribute N as evenly as possible across more items proportional to the amount" in {
    distribute(List(1, 3, 1, 1), 17) shouldBe List(3, 9, 3, 2)
  }

  it should "distribute N as evenly as possible across more items proportional to the amount with zero items at the end" in {
    distribute(List(1, 3, 1, 1, 0, 0, 0), 17) shouldBe List(3, 9, 3, 2, 0, 0, 0)
  }

  it should "distribute N as evenly as possible across three item" in {
    distribute(List(1, 1, 1), 97) shouldBe List(33, 32, 32)
  }

  it should "distribute N proportional to the input list in a simple case" in {
    distribute(List(1, 2), 90) shouldBe List(30, 60)
  }

  it should "distribute N proportional to the input list in a complex case #1" in {
    distribute(List(1, 1, 1, 1, 1), 103) shouldBe List(21, 21, 21, 20, 20)
  }

  it should "distribute N proportional to the input list in a complex case #2" in {
    distribute(List(1, 2, 1, 1), 103) shouldBe List(21, 41, 21, 20)
  }

  it should "work with large numbers x" in {
    val n = 1000000000 // little less than half of max Int
    distribute(List(1 * n, 2 * n, 1 * n), 4) shouldBe List(1, 2, 1)
  }

}
