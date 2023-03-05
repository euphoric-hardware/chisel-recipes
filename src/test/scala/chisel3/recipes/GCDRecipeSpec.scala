package chisel3.recipes

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
  //io.outputGCD := 1.U
  //io.outputValid := 0.B

  val x = Reg(UInt())
  val y = Reg(UInt())
  //io.outputValid := 0.U
  io.outputGCD := x
  io.outputValid := y === 0.U

  forever(
    waitUntil(io.loadingValues === true.B),
    action {
      x := io.value1
      y := io.value2
    },
    whileLoop(y > 0.U)(
      action {
        when(x > y) {
          x := x - y
        }.otherwise {
          y := y - x
        }
      },
      tick
    ),
    tick
  ).compile()
}

class GCDRecipeSpec extends AnyFreeSpec with ChiselScalatestTester {
  "gcd recipe" in {
    test(new GCDRecipe()).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      c.io.value1.poke(8.U)
      c.io.value2.poke(4.U)
      c.io.loadingValues.poke(1.U)
      c.clock.step(1)
      c.io.loadingValues.poke(0.U)

      while(!c.io.outputValid.peek().litToBoolean) {
        c.clock.step()
      }
      println(c.io.outputGCD.peek())
      c.clock.step(5)
    }
  }
}
