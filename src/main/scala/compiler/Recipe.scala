package compiler

import chisel3._
import chisel3.util._

object Recipe {
  def compile(r: Recipe, c: Clock): Unit = {
    r match {
      s: Sequential(recipes) =>
        val ticks = countTicks(recipes)
        val stateReg = RegInit(UInt(log2Ceil(ticks).W, 0.U))
        stateReg := stateReg + 1.U
        when()
      _ => ???
    }
  }

  def countTicks(r: Seq[Recipe]): Int = {

  }
}
sealed trait Recipe
//case class Skip(next: Recipe) extends Recipe
case object Tick extends Recipe
case class Action(a: () => Unit) extends Recipe
case class Sequential(recipes: Seq[Recipe]) extends Recipe
//case class Parallel(recipes: List[Recipe]) extends Recipe
//case class Wait(cond: Boolean) extends Recipe
//case class When(cond: Boolean, expr: Recipe) extends Recipe
case class IfThenElse(cond: Boolean, thenCase: Recipe, elseCase: Recipe) extends Recipe
case class While(cond: Boolean, loop: Recipe) extends Recipe
//case class Background(recipe: Recipe) extends Recipe
