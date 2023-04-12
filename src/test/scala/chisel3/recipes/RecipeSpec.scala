package chisel3.recipes

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class RecipeSpec extends AnyFreeSpec with ChiselScalatestTester {
  abstract class Example extends Module {
    val io = IO(new Bundle {
      val out = Output(UInt(8.W))
    })
  }

  "Should compile/run very basic recipe" in {
    test(new Example {
      io.out := 100.U
      recipe (
        action { io.out := 10.U },
        tick,
        action { io.out := 0.U },
        tick,
        action { io.out := 20.U }
      ).compile(CompileOpts.debugAll)
    }).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.out.expect(10.U)
      dut.clock.step()
      dut.io.out.expect(0.U)
      dut.clock.step()
      dut.io.out.expect(20.U)
      dut.clock.step()
      dut.io.out.expect(100.U)
    }
  }

  "Recipes should support nested sequentials" in {
    test(new Example {
        io.out := 100.U
        recipe (
          recipe (
            action { io.out := 10.U },
            tick
          ),
          recipe (
            action { io.out := 0.U },
            tick
          ),
          action { io.out := 20.U }
        ).compile(CompileOpts.debugAll)
    }).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.out.expect(10.U)
      dut.clock.step()
      dut.io.out.expect(0.U)
      dut.clock.step()
      dut.io.out.expect(20.U)
      dut.clock.step()
      dut.io.out.expect(100.U)
    }
  }

  "Basic while loop" in {
    test(new Example {
      val r = RegInit(UInt(8.W), 0.U)
      io.out := r

      whileLoop(r < 10.U)(
        action { r := r + 1.U },
        tick
      ).compile(CompileOpts.debug)
    }).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      for (i <- 0 until 10) {
        dut.io.out.expect(i.U)
        dut.clock.step()
      }
      dut.clock.step()
      dut.io.out.expect(10.U)
    }
  }

  "While loop with a terminating action" in {
    test(new Example {
      val r = RegInit(UInt(8.W), 0.U)
      io.out := r

      recipe (
        whileLoop(r < 10.U)(
          action { r := r + 1.U },
          tick
        ),
        action { io.out := 2.U * r }
      ).compile(CompileOpts.debug)
    }).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      for (i <- 0 until 10) {
        dut.io.out.expect(i.U)
        dut.clock.step()
      }
      dut.io.out.expect(20.U) // the 2x assignment to io.out is combinational
      dut.clock.step()
      dut.io.out.expect(10.U) // io.out should revert back to 10 after the recipe completes
    }
  }

  "while loop with immediate passthrough" in {
    test(new Example {
      io.out := 1.U
      val w = WireDefault(0.B)
      recipe (
        whileLoop(w)( // condition is immediately false
          tick // shouldn't execute
        ),
        action { io.out := 100.U } // should get here combinationally
      ).compile(CompileOpts.debug)
    }) { dut =>
      dut.io.out.expect(100.U)
      dut.clock.step(1)
      dut.io.out.expect(1.U) // recipe is over, io.out should snap back to default
    }
  }

  "waitUntil with immediate completion" in {
    test(new Example {
      io.out := 1.U
      val w = WireDefault(1.U)
      recipe (
        waitUntil(w === 1.U),
        action { io.out := 100.U }
      ).compile(CompileOpts.debugAll)
    }).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.out.expect(100.U)
      dut.clock.step(1)
      dut.io.out.expect(1.U)
      dut.clock.step(1)
    }
  }

  abstract class ActiveSignalExample extends Module {
    val io = IO(new Bundle {
      val out = Output(Bool())
    })
  }

  "active signal with single-cycle loop" in {
    test(new ActiveSignalExample {

      val r = RegInit(UInt(8.W), 0.U)
      whileLoop(r < 5.U, io.out)(
        action {
          r := r + 1.U
        },
        tick
      ).compile(CompileOpts.debugAll)
    }).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.out.expect(true)

      for (_ <- 0 until 4) {
        dut.clock.step(1)
        dut.io.out.expect(true)
      }
      dut.clock.step(1)
      dut.io.out.expect(false)
      dut.clock.step(1)
      dut.io.out.expect(false)
    }
  }

  "active signal with multi-cycle loop" in {
    test(new ActiveSignalExample {

      val r = RegInit(UInt(8.W), 0.U)
      whileLoop(r < 5.U, io.out)(
        action {
          r := r + 1.U
        },
        tick,
        tick,
      ).compile(CompileOpts.default)
    }).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.out.expect(true)

      for (_ <- 0 until 8) {
        dut.clock.step(1)
        dut.io.out.expect(true)
      }
    }
  }

  "simple doWhile" in {
    test(new ActiveSignalExample {
      val r = RegInit(UInt(8.W), 0.U)
      doWhile(r < 5.U, io.out)(
        action {
          r := r + 1.U
        },
        tick,
        tick,
      ).compile(CompileOpts.default)
    }).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.out.expect(true)

      for (_ <- 0 until 10) {
        dut.clock.step(1)
        dut.io.out.expect(true)
      }
    }
  }

  /*
  "Basic if-then-else statement" in {
    class ITEExample extends Module {
      val io = IO(new Bundle {
        val in = Input(UInt(8.W))
        val out = Output(UInt(8.W))
      })

      io.out := 0.U
      val r: Recipe = IfThenElse(
        io.in < 10.U,
        Action(() => io.out := 2.U),
        Action(() => io.out := 5.U),
      )
      Recipe.compile(r)
    }
    test(new ITEExample()).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.in.poke(8.U)
      dut.io.out.expect(2.U)
      dut.io.in.poke(12.U)
      dut.io.out.expect(5.U)
    }
  }
 */
}
