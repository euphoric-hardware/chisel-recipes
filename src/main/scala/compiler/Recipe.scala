package compiler

import chisel3._
import chisel3.util._

class Recipe {
  def compile(c: Clock): Unit = {
    val ticks = countTicks()
    val state_bits = log2Ceil(ticks)
    val stateReg = RegInit(UInt(state_bits.W), 0.U)
    this match {
      case Sequential(recipes) =>
        for ((r, i) <- recipes.zipWithIndex) {
          when(stateReg === i.U) {
            r.compile(c)
          }
        }
      case Tick =>
        stateReg := Mux(stateReg === state_bits.U, state_bits.U, stateReg + 1.U)
      case Action(a) => a()
    }
  }

  private def countTicks(): Int = {
    this match {
      case Sequential(recipes) =>
        recipes.count(x => x == Tick)
      case _ => 0
    }
  }
}
//case class Skip(next: Recipe) extends Recipe
case object Tick extends Recipe
case class Action(a: () => Unit) extends Recipe
case class Sequential(recipes: Seq[Recipe]) extends Recipe
//case class Parallel(recipes: List[Recipe]) extends Recipe
//case class Wait(cond: Boolean) extends Recipe
//case class When(cond: Boolean, expr: Recipe) extends Recipe
//case class IfThenElse(cond: Boolean, thenCase: Recipe, elseCase: Recipe) extends Recipe
//case class While(cond: Boolean, loop: Recipe) extends Recipe
//case class Background(recipe: Recipe) extends Recipe
