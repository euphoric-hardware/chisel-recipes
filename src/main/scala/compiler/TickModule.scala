package compiler

import chisel3._

class TickModule extends Module {
  val io = IO(new Bundle {
    val go = Input(Bool())
    val done = Output(Bool())
  })

  private val doneReg = RegInit(Bool(), io.go)
  io.done := doneReg
}
