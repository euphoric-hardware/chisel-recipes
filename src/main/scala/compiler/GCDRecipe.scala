package compiler

import chisel3._

class GCDRecipe extends Module {
  val io = IO(new Bundle {
    val load = Input(UInt())
    val valid = Input(UInt())
    val a = Input(UInt(64.W))
    val b = Input(UInt(64.W))

    val out = Output(UInt(64.W))
  })

  val x = Reg(UInt())
  val y = Reg(UInt())
  val clk = Reg(UInt(8.W))

  val r: Recipe =
    IfThenElse(io.load == true.B,
      Sequential(Seq(
      Action { () =>
          x := io.a
          y := io.b
      },
      Tick)),
      While((!io.valid).litToBoolean,
        Sequential(Seq(Action { () =>
            when(x > y) {
                x := x - y
            }.otherwise {
                y := y - x
            }
          },
          Action { () =>
            io.out := x
            io.valid := y === 0.U
          },
          Tick)
        )
      )
    )

  def compile(r: Recipe): Unit = {
    r match {
      case Tick => clk := clk + 1.U
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
}
