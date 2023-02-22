package compiler

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class CompilerSpec extends AnyFreeSpec with ChiselScalatestTester {

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
