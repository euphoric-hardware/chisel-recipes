package compiler

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class CompilerSpec extends AnyFreeSpec with ChiselScalatestTester {
  "action circuit should work" in {
    class ActionExample extends Module {
      val io = IO(new Bundle {
        val x = Output(UInt(8.W))
        val go = Input(Bool())
        val done = Output(Bool())
      })

      io.x := 0.U // default value
      val action = Recipe.actionModule(Action{() => io.x := 10.U})
      io.done := action(io.go)
    }
    test(new ActionExample()) { c =>
      c.io.x.expect(0.U)

      c.clock.step(1)

      c.io.x.expect(0.U)
      c.io.go.poke(1.B)
      c.io.x.expect(10.U)
      c.io.done.expect(1.B)

      c.clock.step(1)

      c.io.x.expect(10.U)
      c.io.done.expect(1.B)
      c.io.go.poke(0.B)
      c.io.x.expect(0.U)
      c.io.done.expect(0.B)
    }
  }

  "tick circuit should work" in {
    class TickExample extends Module {
      val io = IO(new Bundle {
        val go = Input(Bool())
        val done = Output(Bool())
      })

      val tick = Recipe.tickModule
      io.done := tick(io.go)
    }
    test(new TickExample()) { c =>
      c.io.done.expect(0.B)

      c.clock.step(1)

      c.io.done.expect(0.B)
      c.io.go.poke(1.B)
      c.io.done.expect(0.B)

      c.clock.step(1)

      c.io.done.expect(1.B)
      c.io.go.poke(0.B)
      c.io.done.expect(1.B)

      c.clock.step(1)

      c.io.done.expect(0.B)
    }
  }

  "sequential circuit should work" in {
    class SeqExample extends Module {
      val io = IO(new Bundle {
        val go = Input(Bool())
        val done = Output(Bool())
      })
      val seq = Recipe.sequentialModule(Seq(Tick, Tick))
      io.done := seq(io.go)
    }
    test(new SeqExample()) { c =>
      c.io.done.expect(0.B)

      c.clock.step()
      c.io.done.expect(0.B)
      c.io.go.poke(1.B)
      c.io.done.expect(0.B)

      c.clock.step()
      c.io.go.poke(0.B)
      c.io.done.expect(0.B)

      c.clock.step()
      c.io.done.expect(1.B)

      c.clock.step()
      c.io.done.expect(0.B)
    }
  }

  "Should compile/run very basic recipe" in {
    test(new BasicAssignment()).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.a.expect(10.U)
      dut.clock.step()
      dut.io.a.expect(0.U)
      dut.clock.step()
      dut.io.a.expect(20.U)
    }
  }
  // TODO: scoping issues and bare chisel API calls
}
