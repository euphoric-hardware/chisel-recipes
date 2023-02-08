package compiler

import chisel3._

class BasicAssignment extends Module {
  val io = IO(new Bundle {
    val a = Output(UInt(8.W))
  })

  //val clk = Reg(UInt(8.W))
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

  def compile(r: Recipe): Unit = {
    r match {
      case Tick => clock := !clock
      case Action(a) => a()
      case Sequential(recipes) =>
        for (r <- recipes) {
          compile(r)
        }
      case IfThenElse(cond, thenCase, elseCase) =>
        if (cond) {
          compile(thenCase)
        } else {
          compile(elseCase)
        }
      case While(cond, loop) =>
        while (cond) {
          compile(loop)
        }
    }
  }

  io.a := 20.U
}
