package chisel3.recipes

import chisel3._
import chisel3.util.experimental.forceName
import sourcecode.{FileName, Line, Enclosing}

object Recipe {
  private type RecipeModule = Bool => (Bool, Bool) // go: Bool => (done: Bool, active: Bool)

  private def debugPrint(cycleCounter: UInt, entity: String, event: String, debugInfo: DebugInfo): Unit = {
    chisel3.printf(cf"time=[$cycleCounter] [$entity] $event (${debugInfo.fileName.value}:${debugInfo.line.value}) ${debugInfo.enclosing.value}\n")
  }

  private def canonicalName(entity: String, event: String, debugInfo: DebugInfo): String = {
    val scalaFile = debugInfo.fileName.value.split('.').head
    val scalaFileLine = debugInfo.line.value
    val name = s"${entity}_${event}_${scalaFile}_$scalaFileLine"
    name
  }

  private[recipes] def tickModule(tick: Tick, cycleCounter: UInt, compileOpts: CompileOpts): RecipeModule = go => {
    val doneReg = RegInit(Bool(), 0.B)
    doneReg := go

    if (compileOpts.debugWires) {
      val goName = canonicalName(tick.d.entity, "go", tick.d)
      val namedGo = WireDefault(go).suggestName(goName)
      forceName(namedGo, goName)
      dontTouch(namedGo)

      val doneName = canonicalName(tick.d.entity, "done", tick.d)
      val namedDone = WireDefault(doneReg).suggestName(doneName)
      forceName(namedDone, doneName)
      dontTouch(namedDone)
    }

    if (compileOpts.debugPrints.isDefined) {
      when(go) {
        debugPrint(cycleCounter, tick.d.entity, "about to tick", tick.d)
      }
      /*
      when(doneReg) {
        debugPrint(cycleCounter, "Tick", "completed", tick.d)
      }
       */
    }

    tick.active := go
    (doneReg, go)
  }

  private[recipes] def actionModule(action: Action, cycleCounter: UInt, compileOpts: CompileOpts): RecipeModule = go => {
    when(go) {
      action.a()
    }

    if (compileOpts.debugWires) {
      val goName = canonicalName(action.d.entity, "go", action.d)
      val namedGo = WireDefault(go).suggestName(goName)
      forceName(namedGo, goName)
      dontTouch(namedGo)

      val doneName = canonicalName(action.d.entity, "done", action.d)
      val namedDone = WireDefault(go).suggestName(doneName)
      forceName(namedDone, doneName)
      dontTouch(namedDone)
    }

    if (compileOpts.debugPrints.isDefined) {
      when(go) {
        debugPrint(cycleCounter, action.d.entity, "is active", action.d)
      }
    }

    action.active := go
    (go, go)
  }

  private[recipes] def sequentialModule(sequential: Sequential, cycleCounter: UInt, compileOpts: CompileOpts): RecipeModule = go => {
    val recipeMods: Seq[RecipeModule] = sequential.recipes.map(r => compileNoPulse(r, cycleCounter, compileOpts))
    val done = recipeMods.foldLeft(go) { case (g, r) =>
      r(g)._1
    }

    if (compileOpts.debugWires) {
      val goName = canonicalName(sequential.d.entity, "go", sequential.d)
      val namedGo = WireDefault(go).suggestName(goName)
      //forceName(namedGo, goName)
      dontTouch(namedGo)

      val doneName = canonicalName(sequential.d.entity, "done", sequential.d)
      val namedDone = WireDefault(done).suggestName(doneName)
      //forceName(namedDone, doneName)
      dontTouch(namedDone)
    }

    if (compileOpts.debugPrints.isDefined && compileOpts.debugPrints.get.printBlocks) {
      when(go) {
        debugPrint(cycleCounter, sequential.d.entity, "has started", sequential.d)
      }
      when(done) {
        debugPrint(cycleCounter, sequential.d.entity, "has finished", sequential.d)
      }
    }

    sequential.active := !done
    (done, !done)
  }

  private[recipes] def whileModule(w: While, cycleCounter: UInt, compileOpts: CompileOpts): RecipeModule = go => {
    val active = RegInit(Bool(), 0.B)
    val bodyCircuit = compileNoPulse(w.loop, cycleCounter, compileOpts)
    val bodyGo = Wire(Bool())
    val bodyDone = bodyCircuit(bodyGo)._2
    bodyGo := w.cond && (go || bodyDone)
    val done = WireDefault(!w.cond && (bodyDone || go))

    when(done) {
      active := 0.B
    }.elsewhen(bodyGo) {
      active := 1.B
    }

    if (compileOpts.debugWires) {
      val goName = canonicalName(w.d.entity, "go", w.d)
      val namedGo = WireDefault(go).suggestName(goName)
      //forceName(namedGo, goName)
      dontTouch(namedGo)

      val doneName = canonicalName(w.d.entity, "done", w.d)
      val namedDone = WireDefault(done).suggestName(doneName)
      //forceName(namedDone, doneName)
      dontTouch(namedDone)
    }

    if (compileOpts.debugPrints.isDefined) {
      when(go) {
        debugPrint(cycleCounter, w.d.entity, "has started", w.d)
      }
      when(done) {
        debugPrint(cycleCounter, w.d.entity, "has finished", w.d)
      }
    }

    w.active := Mux(done, 0.B, active || (go && !done))
    (done, Mux(done, 0.B, active || (go && !done)))
  }

  private[recipes] def ifThenElseModule(i: IfThenElse, cycleCounter: UInt, compileOpts: CompileOpts): RecipeModule = go => {
    val done = RegInit(Bool(), 0.B)
    val active = Wire(Bool())
    when(i.cond) {
      val execCircuit = compileNoPulse(i.thenCase, cycleCounter, compileOpts)
      val tuple = execCircuit(go)
      done := tuple._1
      active := tuple._2
    }.otherwise {
      val execCircuit = compileNoPulse(i.elseCase, cycleCounter, compileOpts)
      val tuple = execCircuit(go)
      done := tuple._1
      active := tuple._2
    }

    if (compileOpts.debugWires) {
      val goName = canonicalName(i.d.entity, "go", i.d)
      val namedGo = WireDefault(go).suggestName(goName)
      //forceName(namedGo, goName)
      dontTouch(namedGo)

      val doneName = canonicalName(i.d.entity, "done", i.d)
      val namedDone = WireDefault(done).suggestName(doneName)
      //forceName(namedDone, doneName)
      dontTouch(namedDone)
    }

    if (compileOpts.debugPrints.isDefined) {
      when(go) {
        debugPrint(cycleCounter, i.d.entity, "has started", i.d)
      }
      when(done) {
        debugPrint(cycleCounter, i.d.entity, "has finished", i.d)
      }
    }

    (done, active)
  }

  private[recipes] def whenModule(w: When, cycleCounter: UInt, compileOpts: CompileOpts): RecipeModule = go => {
    val done = RegInit(Bool(), 0.B)
    val active = WireInit(Bool(), 0.B)
    when(w.cond) {
      val execCircuit = compileNoPulse(w.body, cycleCounter, compileOpts)
      val tuple = execCircuit(go)
      done := tuple._1
      active := tuple._2
    }

    if (compileOpts.debugWires) {
      val goName = canonicalName(w.d.entity, "go", w.d)
      val namedGo = WireDefault(go).suggestName(goName)
      //forceName(namedGo, goName)
      dontTouch(namedGo)

      val doneName = canonicalName(w.d.entity, "done", w.d)
      val namedDone = WireDefault(done).suggestName(doneName)
      //forceName(namedDone, doneName)
      dontTouch(namedDone)
    }

    if (compileOpts.debugPrints.isDefined) {
      when(go) {
        debugPrint(cycleCounter, w.d.entity, "has started", w.d)
      }
      when(done) {
        debugPrint(cycleCounter, w.d.entity, "has finished", w.d)
      }
    }

    (done, active)
  }

  private def compileNoPulse(r: Recipe, cycleCounter: UInt, compileOpts: CompileOpts): RecipeModule = {
    r match {
      case s @ Sequential(_, _, _) =>
        sequentialModule(s, cycleCounter, compileOpts)
      case t @ Tick(_, _) =>
        tickModule(t, cycleCounter, compileOpts)
      case a @ Action(_, _, _) =>
        actionModule(a, cycleCounter, compileOpts)
      case w @ While(_, _, _, _) =>
        whileModule(w, cycleCounter, compileOpts)
      case i @ IfThenElse(_, _, _, _, _) =>
        ifThenElseModule(i, cycleCounter, compileOpts)
      case w @ When(_, _, _, _) =>
        whenModule(w, cycleCounter, compileOpts)
    }
  }

  def compile(r: Recipe, compileOpts: CompileOpts): (Bool, Bool) = {
    // cycleCounter will be DCE'ed (I hope) if debugPrints is None
    val cycleCounter = RegInit(UInt(32.W), 0.U)
    if (compileOpts.debugPrints.isDefined) {
      cycleCounter := cycleCounter + 1.U
    }

    val recMod = compileNoPulse(r, cycleCounter, compileOpts)
    val pulseReg = RegInit(Bool(), 0.B)
    pulseReg := 1.B
    recMod(pulseReg === 0.U)
  }
}

private[recipes] case class DebugInfo(line: Line, fileName: FileName, enclosing: Enclosing, entity: String)

case class DebugPrints(printBlocks: Boolean = false)
case class CompileOpts(debugPrints: Option[DebugPrints], debugWires: Boolean)
object CompileOpts {
  def default: CompileOpts = CompileOpts(None, debugWires = false)
  def debug: CompileOpts = CompileOpts(Some(DebugPrints()), debugWires = true)
  def debugAll: CompileOpts = CompileOpts(Some(DebugPrints(true)), debugWires = true)
}

private[recipes] sealed abstract class Recipe(active: Bool) {
  def compile(compileOpts: CompileOpts = CompileOpts.default): Unit = {
    this.active := Recipe.compile(this, compileOpts)._2
  }
}
private[recipes] case class Tick(d: DebugInfo, active: Bool = Wire(Bool())) extends Recipe(active)
private[recipes] case class Action(a: () => Unit, d: DebugInfo, active: Bool = Wire(Bool())) extends Recipe(active)
private[recipes] case class Sequential(recipes: Seq[Recipe], d: DebugInfo, active: Bool = Wire(Bool())) extends Recipe(active)
private[recipes] case class While(cond: Bool, loop: Recipe, d: DebugInfo, active: Bool = Wire(Bool())) extends Recipe(active)
//case class Skip(next: Recipe) extends Recipe
//case class Parallel(recipes: List[Recipe]) extends Recipe
private[recipes] case class When(cond: Bool, body: Recipe, d: DebugInfo, active: Bool = Wire(Bool())) extends Recipe(active)
private[recipes] case class IfThenElse(cond: Bool, thenCase: Recipe, elseCase: Recipe, d: DebugInfo, active: Bool = Wire(Bool())) extends Recipe(active)
//case class Background(recipe: Recipe) extends Recipe
