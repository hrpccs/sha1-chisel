package sha1

import chisel3._
import chisel3.util._

class sha1shift extends Module{
  val io = IO(new Bundle{
    val in = Input(UInt(32.W))
    val out = Output(UInt(32.W))
    val enabled = Input(Bool())
  })

  val words = Reg(Vec(16, UInt(32.W)))

  io.out := words(0)

  val next_word = Wire(UInt(32.W))
  val xor_word = Wire(UInt(32.W))

  xor_word := words(2) ^ words(7) ^ words(13) ^ words(15)
  next_word := Mux(io.enabled, io.in , Cat(xor_word(30,0),xor_word(31)))

  for(i <- 0 until 15){
    words(i+1) := words(i)
  }

  words(0) := next_word
}
