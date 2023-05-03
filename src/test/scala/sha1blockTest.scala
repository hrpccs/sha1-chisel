package sha1

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import sha1.TestAnnotations.annos


class sha1blockTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "sha1block"
  it should "work" in {
    // simulate a situation that c.io.in is 0x61626380L, then 0x0, 0x0 and so on
    // use a vector to store the time seq value of c.io.in
    //
    val input = Array(0x61626380L) ++ Array.fill(14)(0x0L) ++ Array(0x00000018L) ++ Array.fill(80)(0x0L)
    test(new sha1block).withAnnotations(TestAnnotations.annos) { c =>
      c.io.restart.poke(true.B)
      c.io.h0.poke(0x67452301L)
      c.io.h1.poke(0xEFCDAB89L)
      c.io.h2.poke(0x98BADCFEL)
      c.io.h3.poke(0x10325476L)
      c.io.h4.poke(0xC3D2E1F0L)
      c.clock.step()
      c.io.restart.poke(false.B)
      c.io.nreset.poke(true.B)

      for(i <- 0 until 82){
        if(i < 81){
          c.io.in.poke(input(i))
        }
        c.clock.step()
      }

    }
  }
}