package compiler

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class GCDSpec extends AnyFreeSpec with ChiselScalatestTester {

  "Should compile/run very basic recipe" in {
    test(new GCDRecipe()) { dut =>
      dut.io.a.poke(10.U)
      dut.io.b.poke(4.U)
//      dut.compile(dut.r)
      dut.io.out.expect(2.U)
      dut.clk.expect(2.U)
    }
  }
  // TODO: scoping issues and bare chisel API calls
}
