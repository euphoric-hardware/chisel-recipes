package chisel3

import sourcecode.{Line, FileName, Enclosing}

package object recipes {
  def tick(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    Tick(DebugInfo(line, fileName, enclosing))
  }

  def action(a: => Unit)(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    Action(() => a, DebugInfo(line, fileName, enclosing))
  }

  def sequence(recipes: Recipe*)(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    Sequential(recipes, DebugInfo(line, fileName, enclosing))
  }

  def recipe(recipes: Recipe*)(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    sequence(recipes:_*)(line, fileName, enclosing)
  }

  /*
  def whileLoop(cond: Bool)(body: => Recipe): Recipe = {
    While(cond, body)
  }
   */

  def whileLoop(cond: Bool)(body: Recipe*)(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    While(cond, sequence(body:_*), DebugInfo(line, fileName, enclosing))
  }

  def waitUntil(cond: Bool)(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    whileLoop(cond)(Tick(DebugInfo(line, fileName, enclosing)))(line, fileName, enclosing)
  }

  def forever(r: Recipe*)(implicit line: Line, fileName: FileName, enclosing: Enclosing): Recipe = {
    whileLoop(true.B)(r:_*)(line, fileName, enclosing)
  }
}