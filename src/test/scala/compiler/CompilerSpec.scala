package compiler

import chisel3._
import chiseltest._
import compiler.Recipe.compile
import org.scalatest.freespec.AnyFreeSpec

class CompilerSpec extends AnyFreeSpec with ChiselScalatestTester {
  abstract class Example extends Module {
    val io = IO(new Bundle {
      val out = Output(UInt(8.W))
    })
  }

  "Should compile/run very basic recipe" in {
    test(new Example {
      io.out := 100.U
      val r: Recipe = Sequential(
        Action { () => io.out := 10.U },
        Tick,
        Action { () => io.out := 0.U },
        Tick,
        Action { () => io.out := 20.U }
      )
      compile(r)
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
        val r: Recipe = Sequential(
          Sequential(
            Action { () => io.out := 10.U },
            Tick
          ),
          Sequential(
            Action { () => io.out := 0.U },
            Tick
          ),
          Action { () => io.out := 20.U }
        )
        compile(r)
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

      val recipe: Recipe = While(
        r < 10.U,
        Sequential(
          Action(() => {r := r + 1.U}),
          Tick
        )
      )
      compile(recipe)
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

      val recipe: Recipe = Sequential(
        While(r < 10.U,
          Sequential(
            Action (() => r := r + 1.U),
            Tick
          )
        ),
        Action(() => io.out := 2.U * r)
      )
      compile(recipe)
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

  "GCD test" in {
    test(new GCDRecipe()).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.loadingValues.poke(1.B)
      dut.io.value1.poke(48.U)
      dut.io.value2.poke(32.U)
      dut.clock.step()
      dut.io.loadingValues.poke(0.B)
      dut.clock.step(5)
      dut.io.outputValid.expect(1.B)
      dut.io.outputGCD.expect(16.U)
    }
  }

 */
}
