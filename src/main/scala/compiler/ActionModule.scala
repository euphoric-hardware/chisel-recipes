package compiler

import chisel3._

class ActionModule(action: () => Unit) extends Module {
  val io = IO(new Bundle {
    val go = Input(Bool())
    val done = Output(Bool())
  })

  private val doneReg = RegInit(Bool(), io.go)
  when(io.go) {
    action()
  }
  io.done := doneReg
}
