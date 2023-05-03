package sha1

import chisel3._
import chisel3.util._
import firrtl.PrimOps.Pad

class sha1block extends Module{
  val io = IO(new Bundle{
    val nreset = Input(Bool())
    val restart = Input(Bool())
    val h0 = Input(UInt(32.W))
    val h1 = Input(UInt(32.W))
    val h2 = Input(UInt(32.W))
    val h3 = Input(UInt(32.W))
    val h4 = Input(UInt(32.W))
    val in = Input(UInt(32.W))

    val a = Output(UInt(32.W))
    val b = Output(UInt(32.W))
    val c = Output(UInt(32.W))
    val d = Output(UInt(32.W))
    val e = Output(UInt(32.W))
    val read_address = Output(UInt(4.W))
    val ready = Output(Bool())
  })

  withReset(!io.nreset){

    val t = RegInit(0.U(7.W))
    val a = RegInit(0.U(32.W))
    val b = RegInit(0.U(32.W))
    val c = RegInit(0.U(32.W))
    val d = RegInit(0.U(32.W))
    val e = RegInit(0.U(32.W))

    io.a := a
    io.b := b
    io.c := c
    io.d := d
    io.e := e

    io.ready := t === 81.U
    io.read_address := t(3,0)

    val sha1shift = Module(new sha1shift)
    val sha1round = Module(new sha1round)

    sha1shift.io.enabled := t(6,4) === 0.U
    sha1shift.io.in := io.in
    sha1round.io.t := t
    sha1round.io.a := a
    sha1round.io.b := b
    sha1round.io.c := c
    sha1round.io.d := d
    sha1round.io.e := e
    sha1round.io.w := sha1shift.io.out

    when(!io.nreset){
      t := 0.U
    }.elsewhen(io.restart){
      t := 0.U
    }.elsewhen(!io.ready){
      t := t + 1.U
    }

    when(t === 0.U){
      a := io.h0
      b := io.h1
      c := io.h2
      d := io.h3
      e := io.h4
    }.otherwise{
      when(!io.ready){
        a := sha1round.io.out(159,128)
        b := sha1round.io.out(127,96)
        c := sha1round.io.out(95,64)
        d := sha1round.io.out(63,32)
        e := sha1round.io.out(31,0)
      }
    }
  }
}
