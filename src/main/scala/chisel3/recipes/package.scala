package chisel3

package object recipes {
  def tick: Recipe = {
    Tick
  }

  def action(a: => Unit): Recipe = {
    Action(() => a)
  }

  def sequence(recipes: Recipe*): Recipe = {
    Sequential(recipes:_*)
  }

  def recipe(recipes: Recipe*): Recipe = {
    sequence(recipes:_*)
  }

  /*
  def whileLoop(cond: Bool)(body: => Recipe): Recipe = {
    While(cond, body)
  }
   */

  def whileLoop(cond: Bool)(body: Recipe*): Recipe = {
    While(cond, sequence(body:_*))
  }

  def waitUntil(cond: Bool): Recipe = {
    whileLoop(cond)(Tick)
  }

  def forever(r: Recipe): Recipe = {
    whileLoop(true.B)(r)
  }
}