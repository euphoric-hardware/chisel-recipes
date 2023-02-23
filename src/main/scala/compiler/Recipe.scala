package compiler

import chisel3._

object Recipe {
  type RecipeModule = Bool => Bool
  private[compiler] val tickModule: RecipeModule = (go: Bool) => {
    val doneReg = RegInit(Bool(), 0.B)
    doneReg := go
    doneReg
  }
  private[compiler] def actionModule(action: Action): RecipeModule = (go: Bool) => {
    when(go) {
      action.a()
    }
    go
  }

  private[compiler] def sequentialModule(recipes: Seq[Recipe]): RecipeModule = (go: Bool) => {
    val recipeMods: Seq[RecipeModule] = recipes.map(compileNoPulse)
    val done = recipeMods.foldLeft(go) { case (go, r) =>
      r(go)
    }
    done
  }

  private def compileNoPulse(r: Recipe): RecipeModule = {
    r match {
      case Sequential(recipes) =>
        sequentialModule(recipes)
      case Tick =>
        tickModule
      case a @ Action(_) =>
        actionModule(a)
    }
  }

  def compile(r: Recipe): Bool = {
    val recMod = compileNoPulse(r)
    val pulseReg = RegInit(Bool(), 0.B)
    pulseReg := 1.B
    recMod(pulseReg === 0.U)
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
//case class IfThenElse(cond: Boolean, thenCase: Recipe, elseCase: Recipe) extends Recipe
//case class While(cond: Boolean, loop: Recipe) extends Recipe
//case class Background(recipe: Recipe) extends Recipe
