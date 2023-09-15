package chisel.recipes

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class GCDRecipe extends Module {
  val io = IO(new Bundle {
    val value1 = Input(UInt(16.W))
    val value2 = Input(UInt(16.W))
    val loadingValues = Input(Bool())
    val outputGCD = Output(UInt(16.W))
    val outputValid = Output(Bool())
  })
  val x = Reg(UInt())
  val y = Reg(UInt())
  val wireValid = Wire(Bool())
  io.outputGCD := x
  io.outputValid := !wireValid

  forever(
    waitUntil(io.loadingValues === true.B),
    action {
      x := io.value1
      y := io.value2
    },
    tick(),
    whileLoop(y > 0.U, wireValid)(
      action {
        when(x > y) {
          x := x - y
        }.otherwise {
          y := y - x
        }
      },
      tick()
    ),
  ).compile(CompileOpts.debug)
}

class GCDRecipeSpec extends AnyFreeSpec with ChiselScalatestTester {
  "gcd recipe" in {
    test(new GCDRecipe()).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      def evaluate(x: Int, y: Int): Unit = {
        c.io.value1.poke(x.U)
        c.io.value2.poke(y.U)
        c.io.loadingValues.poke(1.B)
        c.clock.step(1)
        c.io.loadingValues.poke(0.B)

        while(!c.io.outputValid.peek().litToBoolean) {
          c.clock.step()
        }
        c.io.outputGCD.expect(BigInt(x).gcd(y))
      }

      evaluate(8, 4)
      evaluate(4, 8)
      evaluate(128, 8)
      evaluate(13, 20)
    }
  }
}
