package sha1

import chisel3._

object sha1state extends ChiselEnum {
  val reset,idle,start,running,update = Value
}

class sha1core extends Module {
  val io = IO(new Bundle {
    val nreset = Input(Bool())
    val write_enable = Input(Bool())
    val write_data = Input(UInt(32.W))

    val h0 = Output(UInt(32.W))
    val h1 = Output(UInt(32.W))
    val h2 = Output(UInt(32.W))
    val h3 = Output(UInt(32.W))
    val h4 = Output(UInt(32.W))
    val busy = Output(Bool())
  })
  withReset(!io.nreset){
    val block = RegInit(VecInit(Seq.fill(16)(0.U(32.W))))
    val h0 = RegInit(0x67452301L.U(32.W))
    val h1 = RegInit(0xEFCDAB89L.U(32.W))
    val h2 = RegInit(0x98BADCFEL.U(32.W))
    val h3 = RegInit(0x10325476L.U(32.W))
    val h4 = RegInit(0xC3D2E1F0L.U(32.W))

    val write_address = RegInit(0.U(4.W))
    val read_address = Wire(UInt(4.W))

    val sha1block = Module(new sha1block)
    val state = RegInit(sha1state.reset)

    io.h0 := h0
    io.h1 := h1
    io.h2 := h2
    io.h3 := h3
    io.h4 := h4

    io.busy := state =/= sha1state.idle
    sha1block.io.nreset := io.nreset
    sha1block.io.restart := state === sha1state.start

    read_address := Mux(state === sha1state.start,0.U,sha1block.io.read_address+1.U)
    sha1block.io.in := block(sha1block.io.read_address)

    sha1block.io.h0 := h0
    sha1block.io.h1 := h1
    sha1block.io.h2 := h2
    sha1block.io.h3 := h3
    sha1block.io.h4 := h4

    when(state === sha1state.reset){
      state := sha1state.idle
    }.elsewhen(state === sha1state.idle){
      when(io.write_enable){
        block(write_address) := io.write_data
        when(write_address === 15.U){
          state := sha1state.start
        }.otherwise{
          write_address := write_address + 1.U
        }
      }
    }.elsewhen(state === sha1state.start){
      state := sha1state.running
    }.elsewhen(state === sha1state.running){
      when(sha1block.io.ready){
        state := sha1state.update
      }
    }.elsewhen(state === sha1state.update){
      h0 := h0 + sha1block.io.a
      h1 := h1 + sha1block.io.b
      h2 := h2 + sha1block.io.c
      h3 := h3 + sha1block.io.d
      h4 := h4 + sha1block.io.e
      state := sha1state.idle
    }


  }




}
