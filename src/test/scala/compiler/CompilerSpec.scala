package compiler

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class CompilerSpec extends AnyFreeSpec with ChiselScalatestTester {

  "Should compile/run very basic recipe" in {
    test(new BasicAssignment()) { dut =>
//      dut.compile(dut.r)
      dut.io.a.expect(20.U)
      dut.clk.expect(2.U)
    }
  }
  // TODO: scoping issues and bare chisel API calls
}
