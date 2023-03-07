package chisel3.recipes

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import sourcecode.{Enclosing, FileName, Line}

class RecipeModuleSpec extends AnyFreeSpec with ChiselScalatestTester {
  abstract class RecipeBase extends Module {
    val io = IO(new Bundle {
      val x = Output(UInt(8.W))
      val go = Input(Bool())
      val done = Output(Bool())
    })
    io.x := 0.U // default value
  }

  // This isn't important anyways for the module behavior
  val debugInfo = DebugInfo(implicitly[Line], implicitly[FileName], implicitly[Enclosing])

  "action circuit" in {
    test(new RecipeBase {
      val action = Recipe.actionModule(Action(() => io.x := 10.U, debugInfo), 0.U, CompileOpts.default)
      io.done := action(io.go)
    }) { c =>
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

  "tick circuit" in {
    test(new RecipeBase {
      val tick = Recipe.tickModule(Tick(debugInfo), 0.U, CompileOpts.default)
      io.done := tick(io.go)
    }) { c =>
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

  "tick-only sequential circuit" in {
    test(new RecipeBase {
      val seq = Recipe.sequentialModule(Sequential(Seq(Tick(debugInfo), Tick(debugInfo)), debugInfo), 0.U, CompileOpts.default)
      io.done := seq(io.go)
    }) { c =>
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

  "action-only sequential circuit" in {
    test(new RecipeBase {
      val seq = Recipe.sequentialModule(Sequential(Seq(Action(() => io.x := 10.U, debugInfo)), debugInfo), 0.U, CompileOpts.default)
      io.done := seq(io.go)
    }) { c =>
      c.io.go.poke(1.B)
      c.io.done.expect(1.B)
      c.io.x.expect(10.U)
      c.io.go.poke(0.B)
      c.io.done.expect(0.B)
      c.io.x.expect(0.U)
    }
  }

  "mixed tick and action sequential circuit" in {
    test(new RecipeBase {
      val seq = Recipe.sequentialModule(Sequential(Seq(
        Action(() => io.x := 10.U, debugInfo),
        Tick(debugInfo),
        Tick(debugInfo),
        Action(() => io.x := 8.U, debugInfo),
        Tick(debugInfo)
      ), debugInfo), 0.U, CompileOpts.default)
      io.done := seq(io.go)
    }).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
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

  "while circuit" in {
    test(new RecipeBase {
      val reg = RegInit(UInt(8.W), 0.U)
      io.x := reg
      val whileCircuit = Recipe.whileModule(
        While(cond=reg < 4.U,
          loop=Sequential(Seq(
            Action(() => reg := reg + 1.U, debugInfo),
            Tick(debugInfo)
          ), debugInfo), d=debugInfo),
        0.U, CompileOpts.default)
      io.done := whileCircuit(io.go)
    }).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      c.io.go.poke(1.B)
      c.io.x.expect(0.U)
      c.clock.step(1)
      c.io.go.poke(0.B) // pulse go
      for (i <- 1 to 4) {
        c.io.x.expect(i.U)
        if (i == 4) c.io.done.expect(1.U)
        c.clock.step(1)
      }
      c.io.done.expect(0.U)
      c.io.x.expect(4.U)
      c.clock.step(1)
      c.io.done.expect(0.U)
    }
  }
}
