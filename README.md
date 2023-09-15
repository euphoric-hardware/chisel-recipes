# Chisel Recipes

Write imperative cycle-level code like this:

```scala
// Compute a cumulative sum iteratively from 0 to `range`, when `start` goes high
// Assume that `range` is held steady until `cumsumValid` goes high
class CumulativeSum extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val range = Input(UInt(16.W))
    val cumsum = Output(UInt(24.W))
    val cumsumValid = Output(Bool())
  })
  val sum = Reg(UInt(24.W)) // holds the cumulative sum
  io.cumsum := sum
  val value = Reg(UInt(16.W)) // the current value being added to `sum`
  val valid = RegInit(false.B) // whether cumsum is valid
  io.cumsumValid := valid
  
  // The chisel-recipes eDSL
  forever (
    waitUntil(start === true.B),
    action {
      sum := 0.U
      value := 0.U
      valid := false.B
    },
    tick(),
    whileLoop(value <= range)(
      action {
        sum := sum + value
        value := value + 1.U
      },
      tick()
    ),
    action { valid := true.B }
  ).compile()
}
```

And have it turned automatically into Chisel RTL that implements it!
