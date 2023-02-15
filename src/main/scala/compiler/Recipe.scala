package compiler

import chisel3._
import chisel3.util._

class Recipe {
  def compile(c: Clock): Unit = {
    val ticks = countTicks()
    println(ticks)
    val state_bits = if (ticks == 0) 0 else log2Ceil(ticks+1)
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

  def recipesToActionBlocks(r: Seq[Recipe]): Seq[Seq[Action]] = {
    r.foldLeft(Seq(Seq()))
    r match {
      case Action =>
      case Tick =>
      case _ => ???
    }
  }

  // if the recipe is a Sequential, then aggregate blocks of Actions into a Seq[Seq[Action]]
  // directly emit a state machine just for Seq[Seq[Action]]

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
