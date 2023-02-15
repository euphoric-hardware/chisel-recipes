package compiler

import chisel3._

class BasicAssignment extends Module {
  val io = IO(new Bundle {
    val a = Output(UInt(8.W))
  })

  io.a := 100.U
  val r: Recipe = Sequential(Seq(
    Action {
      () => {
        io.a := 10.U
      }
    },
    Tick,
    Action {
      () => {
        io.a := 0.U
      }
    },
    Tick,
    Action {
      () => {
        io.a := 20.U
      }
    }
  ))
  r.compile(this.clock)
}
