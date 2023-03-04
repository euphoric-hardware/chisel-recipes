package chisel3.recipes

import chisel3._

object Recipe {
  type RecipeModule = Bool => Bool // go: Bool => done: Bool
  private[recipes] val tickModule: RecipeModule = go => {
    val doneReg = RegInit(Bool(), 0.B)
    doneReg := go
    doneReg
  }
  private[recipes] def actionModule(action: Action): RecipeModule = go => {
    when(go) {
      action.a()
    }
    go
  }

  private[recipes] def sequentialModule(recipes: Recipe*): RecipeModule = go => {
    val recipeMods: Seq[RecipeModule] = recipes.map(compileNoPulse)
    val done = recipeMods.foldLeft(go) { case (g, r) =>
      r(g)
    }
    done
  }

  private[recipes] def whileModule(cond: Bool, body: Recipe): RecipeModule = go => {
    val bodyCircuit = compileNoPulse(body)
    val bodyGo = Wire(Bool())
    val bodyDone = bodyCircuit(bodyGo)
    bodyGo := (bodyDone && cond) || go
    !cond && bodyDone
  }

  /*
  private[compiler] def waitUntilModule(cond: Bool): RecipeModule = go => {
    whileModule(cond, Tick)(go)
  }

  private[compiler] def foreverModule(body: Recipe): RecipeModule = go => {
    whileModule(1.B, body)(go)
  }
   */

  private def compileNoPulse(r: Recipe): RecipeModule = {
    r match {
      case Sequential(recipes @ _*) =>
        sequentialModule(recipes:_*)
      case Tick =>
        tickModule
      case a @ Action(_) =>
        actionModule(a)
      case While(cond, loop) =>
        whileModule(cond, loop)
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
case class Sequential(recipes: Recipe*) extends Recipe
//case class Parallel(recipes: List[Recipe]) extends Recipe
//case class Wait(cond: Bool) extends Recipe
//case class When(cond: Bool, body: Recipe) extends Recipe
//case class IfThenElse(cond: Bool, thenCase: Recipe, elseCase: Recipe) extends Recipe
case class While(cond: Bool, loop: Recipe) extends Recipe
//case class Background(recipe: Recipe) extends Recipe
//case class WaitUntil(cond: Bool) extends Recipe
//case class Forever(body: Recipe) extends Recipe
