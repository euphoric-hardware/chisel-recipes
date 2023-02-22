package compiler

import chisel3._
/*
class WhileModule(cond: Bool, inner: RecipeModule) extends RecipeModule {
  val io = IO(new Bundle {
    val go = Input(Bool())
    val doneIn = Input(Bool())

    val doneOut = Output(Bool())
  })

  inner.io.go := io.go || (inner.io.done && !cond)
  io.done := inner.io.done && cond
  val doneReg = RegInit(Bool(), 0.B)
  val prevPulseReg = RegInit(Bool(), 0.B)
  val pulseReg = RegInit(Bool(), 0.B)
  val pulse = RegInit(Bool(), 0.B)

  when (!doneReg) {
    // generate one-cycle pulse
    prevPulseReg := pulseReg
    pulseReg := 1.B

    when(prevPulseReg === 0.B && pulseReg === 1.B) {
      pulse := 1.B
    }.otherwise(pulse := 0.B)

    io.doneOut := pulse
    when (io.doneIn && !cond) {
      io.doneOut := pulse.B
      prevPulseReg := 0.B
    }
  }
}*/
