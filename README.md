# Chisel Recipes

Write imperative cycle-level code like this:

```scala
class CumulativeSum extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val range = Input(UInt(16.W))
    val cumsum = Output(UInt(24.W))
  })
  val sum = Reg(UInt(24.W))
  val value = Reg(UInt(16.W))
  io.cumsum := value
  forever (
    waitUntil(start === true.B),
    action {
      sum := 0.U
      value := 0.U
    },
    tick(),
    whileLoop(value <= range)(
      action {
        sum := sum + value
        value := value + 1.U
      },
      tick()
    )
  ).compile()
}
```

And have it turned automatically into Chisel RTL that implements it!
