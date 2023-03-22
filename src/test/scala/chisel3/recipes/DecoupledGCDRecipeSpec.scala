package chisel3.recipes

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.util.Decoupled
import chiseltest._
import chiseltest.experimental.observe
import chiseltest.formal._
import gcd.DecoupledGcd
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.freespec.AnyFreeSpec

class GcdInputBundle(val w: Int) extends Bundle {
  val value1 = UInt(w.W)
  val value2 = UInt(w.W)
}

class GcdOutputBundle(val w: Int) extends Bundle {
  val value1 = UInt(w.W)
  val value2 = UInt(w.W)
  val gcd    = UInt(w.W)
}

class DecoupledGCDRecipe(width: Int) extends Module {
  val input = IO(Flipped(Decoupled(new GcdInputBundle(width))))
  val output = IO(Decoupled(new GcdOutputBundle(width)))

  val xInitial    = Reg(UInt())
  val yInitial    = Reg(UInt())
  val x           = Reg(UInt())
  val y           = Reg(UInt())
  val resultValid = RegInit(Bool(), 0.B)

  input.ready := 0.B
  output.valid := resultValid
  output.bits := DontCare

  forever (
    waitUntil(input.valid),
    action {
      val bundle = input.deq()
      x := bundle.value1
      y := bundle.value2
      xInitial := bundle.value1
      yInitial := bundle.value2
    },
    tick,
    whileLoop(x > 0.U && y > 0.U)(
      action{
        when(x > y) {
          x := x - y
        }.otherwise {
          y := y - x
        }
      },
      tick
    ),
    action {
      output.bits.value1 := xInitial
      output.bits.value2 := yInitial
      when(x === 0.U) {
        output.bits.gcd := y
      }.otherwise {
        output.bits.gcd := x
      }
    },
    waitUntil(output.fire, resultValid),
  ).compile(CompileOpts.debug)
}

class DecoupledGCDRecipeSpec extends AnyFreeSpec with ChiselScalatestTester {
  "decoupled gcd recipe" in {
    test(new DecoupledGCDRecipe(16)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.input.initSource()
      dut.input.setSourceClock(dut.clock)
      dut.output.initSink()
      dut.output.setSinkClock(dut.clock)

      val testValues = for { x <- 0 to 10; y <- 0 to 10} yield (x, y)
      val inputSeq = testValues.map { case (x, y) => new GcdInputBundle(16).Lit(_.value1 -> x.U, _.value2 -> y.U) }
      val resultSeq = testValues.map { case (x, y) =>
        new GcdOutputBundle(16).Lit(_.value1 -> x.U, _.value2 -> y.U, _.gcd -> BigInt(x).gcd(BigInt(y)).U)
      }

      fork {
        // push inputs into the calculator, stall for 11 cycles one third of the way
        val (seq1, seq2) = inputSeq.splitAt(resultSeq.length / 3)
        dut.input.enqueueSeq(seq1)
        dut.clock.step(11)
        dut.input.enqueueSeq(seq2)
      }.fork {
        // retrieve computations from the calculator, stall for 10 cycles one half of the way
        val (seq1, seq2) = resultSeq.splitAt(resultSeq.length / 2)
        dut.output.expectDequeueSeq(seq1)
        dut.clock.step(10)
        dut.output.expectDequeueSeq(seq2)
      }.join()
    }
  }

  class DecoupledGcdFormalSpec(handGCD: => DecoupledGcd, recipeGCD: => DecoupledGCDRecipe) extends Module {
    // create an instance of our DUT and expose its I/O
    val handDUT = Module(handGCD)
    val recipeDUT = Module(recipeGCD)

    val input = IO(chiselTypeOf(handDUT.input))
    input <> handDUT.input
    input <> recipeDUT.input

    val handOutput = IO(chiselTypeOf(handDUT.output))
    handOutput <> handDUT.output
    val recipeOutput = IO(chiselTypeOf(recipeDUT.output))
    recipeOutput <> recipeDUT.output

    // create a cross module binding to inspect internal state
    val handBusy = observe(handDUT.busy)
    val recipeBusy = observe(!recipeDUT.resultValid)

    // do not accept new inputs while busy
    when(handBusy || recipeBusy) {
      assert(!input.fire)
    }

    // only release outputs when busy
    when(handOutput.fire) {
      assert(handOutput.fire)
    }

    when(recipeOutput.fire) {
      assert(recipeOutput.fire)
    }

    // when there was no transactions, busy should not change
    when(past(!input.fire && !handOutput.fire)) {
      assert(stable(handBusy))
    }

    when(past(!input.fire && !recipeOutput.fire)) {
      assert(stable(recipeBusy))
    }

    // when busy changed from 0 to 1, an input was accepted
    when(rose(handBusy) || rose(recipeBusy)) {
      assert(past(input.fire))
    }

    // when busy changed from 1 to 0, an output was transmitted
    when(fell(handBusy)) {
      assert(past(handOutput.fire))
    }

    when(fell(recipeBusy)) {
      assert(past(recipeOutput.fire))
    }
  }
  class FormalGcdSpec extends AnyFlatSpec with ChiselScalatestTester with Formal {
    "GCD" should "pass" in {
      verify(new DecoupledGcdFormalSpec(
        new DecoupledGcd(4),
        new DecoupledGCDRecipe(4)
      ), Seq(BoundedCheck(20)))
    }
  }


}
