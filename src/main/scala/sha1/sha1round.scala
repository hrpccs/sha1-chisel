package sha1

import chisel3._
import chisel3.util._

class sha1round extends Module {
  val io = IO(new Bundle {
    val t = Input(UInt(7.W))
    val a = Input(UInt(32.W))
    val b = Input(UInt(32.W))
    val c = Input(UInt(32.W))
    val d = Input(UInt(32.W))
    val e = Input(UInt(32.W))
    val w = Input(UInt(32.W))

    val out = Output(UInt(160.W))
  })

  val sha1_f = Wire(UInt(32.W))
  val sha1_k = Wire(UInt(32.W))
  val sha1op = Wire(UInt(160.W))

  sha1_k := Mux(io.t <= 20.U, "h5a827999".U,
    Mux(io.t <= 40.U, "h6ed9eba1".U,
      Mux(io.t <= 60.U, "h8f1bbcdc".U,
        "hca62c1d6".U)))
  sha1_f := Mux(io.t <= 20.U, (io.b & io.c) | ((~io.b).asUInt & io.d),
    Mux(io.t <= 40.U, io.b ^ io.c ^ io.d,
      Mux(io.t <= 60.U, (io.b & io.c) | (io.b & io.d) | (io.c & io.d),
        io.b ^ io.c ^ io.d)))

  val na = Wire(UInt(32.W))
  val nc = Wire(UInt(32.W))

  na := Cat(io.a(26,0),io.a(31,27)) + sha1_f + io.e + sha1_k + io.w
  nc := Cat(io.b(1,0),io.b(31,2))

  sha1op := Cat(na,io.a,nc,io.c,io.d)

  io.out := sha1op
}
