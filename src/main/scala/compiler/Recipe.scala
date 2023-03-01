package compiler

import chisel3._

object Recipe {
  type RecipeModule = Bool => Bool // go: Bool => done: Bool
  private[compiler] val tickModule: RecipeModule = go => {
    val doneReg = RegInit(Bool(), 0.B)
    doneReg := go
    doneReg
  }
  private[compiler] def actionModule(action: Action): RecipeModule = go => {
    when(go) {
      action.a()
    }
    go
  }

  private[compiler] def sequentialModule(recipes: Seq[Recipe]): RecipeModule = go => {
    val recipeMods: Seq[RecipeModule] = recipes.map(compileNoPulse)
    val done = recipeMods.foldLeft(go) { case (go, r) =>
      r(go)
    }
    done
  }

  private[compiler] def whileModule(cond: Bool, body: Recipe): RecipeModule = go => {
    val recDone = RegInit(Bool(), 0.B)
    val internalGo = RegInit(Bool(), go)
    val done = RegInit(Bool(), 0.B)
    val recMod = compileNoPulse(body)
    when (internalGo) {
      recDone := recMod(internalGo)
      when (recDone) {
        val pulseReg = RegInit(Bool(), 0.B)
        pulseReg := 1.B
        internalGo := (pulseReg === 0.U)
        when (cond) {
          done := internalGo
        }.otherwise(done := 0.B)
      }
    }
    done
  }

  private[compiler] def ifThenElseModule(cond: Bool, thenCase: Recipe, elseCase: Recipe): RecipeModule = go => {
    val done = RegInit(Bool(), 0.B)
    when (cond) {
      done := compileNoPulse(thenCase)(go)
    }.otherwise(done := compileNoPulse(elseCase)(go))
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
      case While(cond, loop) =>
        whileModule(cond, loop)
      case IfThenElse(cond, thenCase, elseCase) =>
        ifThenElseModule(cond, thenCase, elseCase)
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
case class IfThenElse(cond: Bool, thenCase: Recipe, elseCase: Recipe) extends Recipe
case class While(cond: Bool, loop: Recipe) extends Recipe
//case class Background(recipe: Recipe) extends Recipe
