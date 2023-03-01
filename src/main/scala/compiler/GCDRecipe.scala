package compiler

import chisel3._

class GCDRecipe extends Module {
  val io = IO(new Bundle {
    val value1 = Input(UInt(16.W))
    val value2 = Input(UInt(16.W))
    val loadingValues = Input(Bool())
    val outputGCD = Output(UInt(16.W))
    val outputValid = Output(Bool())
  })

  val x = Reg(UInt())
  val y = Reg(UInt())

  val r1: Recipe = IfThenElse(x > y,
    Action(() => x := x - y),
    Action(() => y := y - x)
  )

  val r2: Recipe = When(io.loadingValues,
    Action (() => {
      x := io.value1
      y := io.value2
    })
  )

  val r3: Recipe = Action(() => {
    io.outputGCD := x
    io.outputValid := y === 0.U
  })

  val r: Recipe = Sequential(Seq(
    r1,
    r2,
    r3
  ))

  Recipe.compile(r)
}
