package chisel3.recipes

import chisel3._
import sourcecode.{FileName, Line, Enclosing}

object Recipe {
  type RecipeModule = Bool => Bool // go: Bool => done: Bool
  private[recipes] def tickModule(tick: Tick, debugPrints: Boolean, cycleCounter: UInt): RecipeModule = go => {
    val doneReg = RegInit(Bool(), 0.B)
    doneReg := go
    if (debugPrints) {
      when(doneReg) {
        chisel3.printf(cf"time=[$cycleCounter] Tick ${tick.d.enclosing.value} (${tick.d.fileName.value}:${tick.d.line.value}) completed\n")
      }
    }
    doneReg
  }

  private[recipes] def actionModule(action: Action, debugPrints: Boolean, cycleCounter: UInt): RecipeModule = go => {
    when(go) {
      action.a()
    }
    if (debugPrints) {
      when(go) {
        chisel3.printf(cf"time=[$cycleCounter] Action ${action.d.enclosing.value} (${action.d.fileName.value}:${action.d.line.value}) is active\n")
      }
    }
    go
  }

  private[recipes] def sequentialModule(recipes: Seq[Recipe], debugPrints: Boolean, cycleCounter: UInt): RecipeModule = go => {
    val recipeMods: Seq[RecipeModule] = recipes.map(r => compileNoPulse(r, debugPrints, cycleCounter))
    val done = recipeMods.foldLeft(go) { case (g, r) =>
      r(g)
    }
    done
  }

  private[recipes] def whileModule(cond: Bool, body: Recipe, debugPrints: Boolean, cycleCounter: UInt): RecipeModule = go => {
    val bodyCircuit = compileNoPulse(body, debugPrints, cycleCounter)
    val bodyGo = Wire(Bool())
    val bodyDone = bodyCircuit(bodyGo)
    bodyGo := cond && (go || bodyDone)
    !cond && (bodyDone || go)
  }

  private def compileNoPulse(r: Recipe, debugPrints: Boolean, cycleCounter: UInt): RecipeModule = {
    r match {
      case Sequential(recipes, _) =>
        sequentialModule(recipes, debugPrints, cycleCounter)
      case t @ Tick(_) =>
        tickModule(t, debugPrints, cycleCounter)
      case a @ Action(_, _) =>
        actionModule(a, debugPrints, cycleCounter)
      case While(cond, loop, _) =>
        whileModule(cond, loop, debugPrints, cycleCounter)
    }
  }

  def compile(r: Recipe, debugPrints: Boolean): Bool = {
    // cycleCounter will be DCE'ed (I hope) if debugPrints is false
    val cycleCounter = RegInit(UInt(32.W), 0.U)
    if (debugPrints) {
      cycleCounter := cycleCounter + 1.U
    }

    val recMod = compileNoPulse(r, debugPrints, cycleCounter)
    val pulseReg = RegInit(Bool(), 0.B)
    pulseReg := 1.B
    recMod(pulseReg === 0.U)
  }
}

private[recipes] case class DebugInfo(line: Line, fileName: FileName, enclosing: Enclosing)
private[recipes] sealed abstract class Recipe(debugInfo: DebugInfo) {
  def compile(debugPrints: Boolean = false): Unit = {
    Recipe.compile(this, debugPrints)
  }
}
private[recipes] case class Tick(d: DebugInfo) extends Recipe(d)
private[recipes] case class Action(a: () => Unit, d: DebugInfo) extends Recipe(d)
private[recipes] case class Sequential(recipes: Seq[Recipe], d: DebugInfo) extends Recipe(d)
private[recipes] case class While(cond: Bool, loop: Recipe, d: DebugInfo) extends Recipe(d)
//case class Skip(next: Recipe) extends Recipe
//case class Parallel(recipes: List[Recipe]) extends Recipe
//case class When(cond: Bool, body: Recipe) extends Recipe
//case class IfThenElse(cond: Bool, thenCase: Recipe, elseCase: Recipe) extends Recipe
//case class Background(recipe: Recipe) extends Recipe
//case class WaitUntil(cond: Bool) extends Recipe
//case class Forever(body: Recipe) extends Recipe
