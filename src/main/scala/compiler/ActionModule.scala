package compiler

import chisel3._
/*
class ActionModule(action: () => Unit) extends RecipeModule {
  io.done := io.go
  //private val doneReg = RegInit(Bool(), io.go)
  when(io.go) {
    action()
  }
  //io.done := doneReg
}*/
