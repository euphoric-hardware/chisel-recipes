# Chisel Recipes

## Usage

Add this to your `build.sbt`:

```sbt
resolvers +=
  "Sonatype OSS Snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots"
```

Then depend on the latest SNAPSHOT artifact in your `build.sbt`

![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/io.github.euphoric-hardware/chisel-recipes_2.13?server=https%3A%2F%2Fs01.oss.sonatype.org)

The full list of [SNAPSHOT releases can be found here](https://s01.oss.sonatype.org/content/repositories/snapshots/io/github/euphoric-hardware/chisel-recipes_2.13/).

```sbt
libraryDependencies ++= Seq(
  "io.github.euphoric-hardware" %% "chisel-recipes" % <VERSION>
)
```

## Example

Write imperative cycle-level code like this:

```scala
import chisel3._
import chisel.recipes._

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
