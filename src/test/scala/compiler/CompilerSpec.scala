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

  "tick-only sequential circuit should work" in {
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

  "action-only sequential circuit should work" in {
    class SeqExample extends Module {
      val io = IO(new Bundle {
        val go = Input(Bool())
        val done = Output(Bool())
        val x = Output(UInt(8.W))
      })
      io.x := 0.U
      val seq = Recipe.sequentialModule(Seq(Action(() => io.x := 10.U)))
      io.done := seq(io.go)
    }
    test(new SeqExample()) { c =>
      c.io.go.poke(1.B)
      c.io.done.expect(1.B)
      c.io.x.expect(10.U)
      c.io.go.poke(0.B)
      c.io.done.expect(0.B)
      c.io.x.expect(0.U)
    }
  }

  "mixed tick and action sequential circuit should work" in {
    class SeqExample extends Module {
      val io = IO(new Bundle {
        val go = Input(Bool())
        val done = Output(Bool())
        val x = Output(UInt(8.W))
      })
      io.x := 0.U
      val seq = Recipe.sequentialModule(Seq(Action(() => io.x := 10.U), Tick, Tick, Action(() => io.x := 8.U), Tick))
      io.done := seq(io.go)
    }
    test(new SeqExample()).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      c.io.go.poke(1.B)
      c.io.x.expect(10.U) // the first action is combinational
      c.clock.step() // first tick
      c.io.go.poke(0.B) // pulse go
      c.io.x.expect(0.U) // first tick
      c.clock.step() // second tick
      c.io.x.expect(8.U) // second tick
      c.clock.step() // final tick
      c.io.done.expect(1.B)
      c.clock.step()
      c.io.done.expect(0.B) // done should pulse
    }
  }

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
      Recipe.compile(r)
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
}
