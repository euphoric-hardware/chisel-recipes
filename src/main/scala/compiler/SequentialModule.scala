package compiler

import chisel3._

class SequentialModule(recipes: Seq[Recipe]) extends Module {
  val io = IO(new Bundle {
    val go = Input(Bool())
    val done = Output(Bool())
  })

  val pulseReg = RegInit(Bool(), 0.B)
  val prevPulseReg = Reg(Bool())
  prevPulseReg := pulseReg
  pulseReg := 1.B

  val pulse = RegInit(Bool(), 0.B)
  when (prevPulseReg === 0.B && pulseReg === 1.B) {
    pulse := 1.B
  }.otherwise(pulse := 0.B)

  recipes[0].io.go = pulse
  for (r <- recipes) {
    r.io.go =
  }
}
