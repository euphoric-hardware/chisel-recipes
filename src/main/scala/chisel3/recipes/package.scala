package chisel3

import sourcecode.{Line, FileName, Enclosing}

package object recipes {
  def tick(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    Tick(DebugInfo(line, fileName, enclosing, "tick"))
  }

  def action(a: => Unit)(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    Action(() => a, DebugInfo(line, fileName, enclosing, "action"))
  }

  def recipe(recipes: Recipe*)(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    Sequential(recipes, DebugInfo(line, fileName, enclosing, "recipe"))
  }

  /*
  def whileLoop(cond: Bool)(body: => Recipe): Recipe = {
    While(cond, body)
  }
   */

  private def whilePrim(cond: Bool, active: Bool = Wire(Bool()))(body: Recipe*)(line: Line, fileName: FileName, enclosing: Enclosing, entity: String): Recipe = {
    While(cond, recipe(body:_*)(line, fileName, enclosing), active, DebugInfo(line, fileName, enclosing, entity))
  }

  def whileLoop(cond: Bool, active: Bool = Wire(Bool()))(body: Recipe*)(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    whilePrim(cond, active)(body:_*)(line, fileName, enclosing, "whileLoop")
  }

  def waitUntil(cond: Bool, active: Bool = Wire(Bool()))(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    whilePrim(!cond, active)(Tick(DebugInfo(line, fileName, enclosing, "waitUntil_tick")))(line, fileName, enclosing, "waitUntil")
  }

  def forever(r: Recipe*)(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    whilePrim(true.B)(r:_*)(line, fileName, enclosing, "forever")
  }

  def ifThenElse(cond: Bool)(t: Recipe)(e: Recipe)(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    IfThenElse(cond, t, e, DebugInfo(line, fileName, enclosing, "ifThenElse"))
  }

  def whenPrim(cond: Bool)(body: Recipe*)(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    When(cond, recipe(body:_*), DebugInfo(line, fileName, enclosing, "when"))
  }
}