package compiler

import chisel3._
/*
class SequentialModule(recipes: Seq[RecipeModule]) extends RecipeModule {
  val pulseReg = RegInit(Bool(), 0.B)
  val prevPulseReg = Reg(Bool())
  prevPulseReg := pulseReg
  pulseReg := 1.B

  val pulse = RegInit(Bool(), 0.B)
  when (prevPulseReg === 0.B && pulseReg === 1.B) {
    pulse := 1.B
  }.otherwise(pulse := 0.B)

  recipes.sliding(2).foreach { recipeGroup =>
    recipeGroup(1).io.go := recipeGroup(0).io.done
  }
  recipes.head.io.go := pulse
  io.done := recipes.last.io.done
}
*/
