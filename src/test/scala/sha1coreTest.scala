package sha1

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class sha1coreTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "sha1core"
  it should "work" in {
    // simulate a situation that c.io.in is 0x61626380L, then 0x0, 0x0 and so on
    // use a vector to store the time seq value of c.io.in
    //
    val input = Array(0x61626380L) ++ Array.fill(14)(0x0L) ++ Array(0x00000018L) ++ Array.fill(80)(0x0L)
    test(new sha1core).withAnnotations(TestAnnotations.annos) { c =>
      c.io.nreset.poke(true.B)
      c.io.write_enable.poke(false.B)
      c.clock.step()

      for(i <- 0 until 16){
        c.io.write_enable.poke(true.B)
        c.io.write_data.poke(input(i))
        c.clock.step()
      }

      c.io.write_enable.poke(false.B)

      for(i <- 0 until 100){
        c.io.write_data.poke(i)
        c.clock.step()
      }




    }
  }
}
