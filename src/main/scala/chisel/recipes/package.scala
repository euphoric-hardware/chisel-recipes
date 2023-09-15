package chisel

import chisel3.{Bool, Wire, fromBooleanToLiteral}
import sourcecode.{Enclosing, FileName, Line}

package object recipes {
  def tick(active: Bool = Wire(Bool()))(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    Tick(DebugInfo(line, fileName, enclosing, "tick"), active)
  }

  def tick(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    Tick(DebugInfo(line, fileName, enclosing, "tick"), Wire(Bool()))
  }

  def action(a: => Unit, active: Bool = Wire(Bool()))(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    Action(() => a, DebugInfo(line, fileName, enclosing, "action"), active)
  }

  def recipe(active: Bool, recipes: Recipe*)(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    Sequential(recipes, DebugInfo(line, fileName, enclosing, "recipe"), active)
  }

  def recipe(recipes: Recipe*)(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    Sequential(recipes, DebugInfo(line, fileName, enclosing, "recipe"), Wire(Bool()))
  }

  private def whilePrim(cond: Bool, active: Bool = Wire(Bool()))(body: Recipe*)(line: Line, fileName: FileName, enclosing: Enclosing, entity: String): Recipe = {
    While(cond, recipe(active, body:_*)(line, fileName, enclosing), DebugInfo(line, fileName, enclosing, entity), active)
  }

  def whileLoop(cond: Bool, active: Bool = Wire(Bool()))(body: Recipe*)(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    whilePrim(cond, active)(body:_*)(line, fileName, enclosing, "whileLoop")
  }

  def waitUntil(cond: Bool, active: Bool = Wire(Bool()))(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    whilePrim(!cond, active)(Tick(DebugInfo(line, fileName, enclosing, "waitUntil_tick"), active))(line, fileName, enclosing, "waitUntil")
  }

  def doWhile(body: Recipe*)(cond: Bool, active: Bool = Wire(Bool()))(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    recipe (
      active,
      recipe(body:_*),
      whilePrim(cond)(body:_*)(line, fileName, enclosing, "doWhile")
    )
  }

  def forever(active: Bool, r: Recipe*)(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    whilePrim(true.B, active)(r:_*)(line, fileName, enclosing, "forever")
  }

  def forever(r: Recipe*)(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    whilePrim(true.B)(r: _*)(line, fileName, enclosing, "forever")
  }

  def ifThenElse(cond: Bool, active: Bool = Wire(Bool()))(t: Recipe)(e: Recipe)(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    IfThenElse(cond, t, e, DebugInfo(line, fileName, enclosing, "ifThenElse"), active)
  }

  def whenPrim(cond: Bool, active: Bool = Wire(Bool()))(body: Recipe*)(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    When(cond, recipe(active, body:_*), DebugInfo(line, fileName, enclosing, "when"), active)
  }
}
