package compiler

import chisel3._
import chiseltest._
import compiler.Recipe.compile
import org.scalatest.freespec.AnyFreeSpec

class CompilerSpec extends AnyFreeSpec with ChiselScalatestTester {


  "Should compile/run very basic recipe" in {
    class BasicAssignment extends Module {
      val io = IO(new Bundle {
        val a = Output(UInt(8.W))
      })

      io.a := 100.U
      val r: Recipe = Sequential(Seq(
        Action { () => io.a := 10.U },
        Tick,
        Action { () => io.a := 0.U },
        Tick,
        Action { () => io.a := 20.U }
      ))
      compile(r)
    }
    test(new BasicAssignment()).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.a.expect(10.U)
      dut.clock.step()
      dut.io.a.expect(0.U)
      dut.clock.step()
      dut.io.a.expect(20.U)
      dut.clock.step()
      dut.io.a.expect(100.U)
    }
  }

  "Recipes should support nested sequentials" in {
    class BasicAssignment extends Module {
      val io = IO(new Bundle {
        val a = Output(UInt(8.W))
      })

      io.a := 100.U
      val r: Recipe = Sequential(Seq(
        Sequential(Seq(
          Action { () => io.a := 10.U },
          Tick
        )),
        Sequential(Seq(
          Action { () => io.a := 0.U },
          Tick
        )),
        Action { () => io.a := 20.U }
      ))
      compile(r)
    }
    test(new BasicAssignment()).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.a.expect(10.U)
      dut.clock.step()
      dut.io.a.expect(0.U)
      dut.clock.step()
      dut.io.a.expect(20.U)
      dut.clock.step()
      dut.io.a.expect(100.U)
    }
  }

  "Basic while loop" in {
    class WhileExample extends Module {
      val io = IO(new Bundle {
        val out = Output(UInt(8.W))
      })
      val r = RegInit(UInt(8.W), 0.U)
      io.out := r

      val recipe: Recipe = While(
        r < 10.U,
        Sequential(Seq(
          Action(() => {r := r + 1.U}),
          Tick
        ))
      )
      compile(recipe)
    }
    test(new WhileExample()).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      for (i <- 0 until 10) {
        dut.io.out.expect(i.U)
        dut.clock.step()
      }
    }
  }

  "While loop should transmit done signal" in {
    class WhileCompound extends Module {
      val io = IO(new Bundle {
        val out = Output(UInt(8.W))
      })
      val r = RegInit(UInt(8.W), 0.U)
      io.out := 0.U

      val recipe: Recipe = Sequential(Seq(
        While(r < 10.U,
          Sequential(Seq(
            Action (() => r := r + 1.U),
          ))
        ),
        Tick,
        Action(() => io.out := 2.U * r)
      ))
      compile(recipe)
    }
    test(new WhileCompound()).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      for (i <- 0 until 10) {
        dut.io.out.expect(i.U)
        dut.clock.step()
      }
      dut.clock.step()
      dut.io.out.expect(20.U)
    }
  }

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
}
