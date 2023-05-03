package sha1

import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import chisel3.util._
import chisel3.{ChiselEnum, _}

object sha1topstate extends ChiselEnum {
  val idle,padding,length,done = Value
}

class sha1top extends Module {
  val io = IO(new Bundle {
    val nreset = Input(Bool())
    val write_enable = Input(Bool())
    val last = Input(Bool())
    val last_len = Input(UInt(2.W))
    val data_in = Input(UInt(32.W))
    val read_address = Input(UInt(3.W))

    val data_out = Output(UInt(32.W))
    val ready = Output(Bool())
    val busy = Output(Bool())
  })

  withReset(!io.nreset){
    val write_data = WireInit(0.U(32.W))
    val write_enable = WireInit(false.B)
    val sha1core = Module(new sha1core)
    sha1core.io.nreset := io.nreset
    sha1core.io.write_enable := write_enable
    sha1core.io.write_data := write_data

    val input_length = Wire(UInt(32.W))
    input_length := Mux(io.last,io.last_len,4.U)

    val length = RegInit(0.U(32.W))
    val pad = RegInit(0.U(32.W))
    val state = RegInit(sha1topstate.idle)
    val wordlen = Wire(UInt(4.W))

    wordlen := length(5,2)

    io.busy := sha1core.io.busy || state === sha1topstate.padding || state === sha1topstate.length
    io.ready := state === sha1topstate.done && !io.busy

    when(!sha1core.io.busy){
      when(state === sha1topstate.idle){
        when(io.write_enable && !io.last){
          length := length + input_length
          pad := 0.U
          state := sha1topstate.idle
        }.elsewhen(io.write_enable && io.last){
          length := length + input_length
          pad := Mux(wordlen === 14.U, 15.U,
            Mux(wordlen === 15.U, 14.U, 13.U - wordlen)
          )
          state := sha1topstate.padding
        }.otherwise{
          length := 0.U
          pad := 0.U
          state := sha1topstate.idle
        }
      }.elsewhen(state === sha1topstate.padding){
        when(pad === 0.U){
          state := sha1topstate.length
        }.otherwise{
          pad := pad - 1.U
        }
      }.elsewhen(state === sha1topstate.length){
        state := sha1topstate.done
      }.otherwise{
        state := sha1topstate.done
      }
    }

    when(state === sha1topstate.idle){
      write_enable := io.write_enable
      write_data := Mux(!io.last,io.data_in,
        MuxCase(0.U,Array(
          (io.last_len === 0.U) -> 0x80000000L.U,
          (io.last_len === 1.U) -> Cat(io.data_in(31,24),0x800000L.U),
          (io.last_len === 2.U) -> Cat(io.data_in(31,16),0x8000L.U),
          (io.last_len === 3.U) -> Cat(io.data_in(31,8),0x80L.U)
        ))
      )
    }.elsewhen(state === sha1topstate.padding){
      write_enable := true.B
      write_data := 0.U
    }.elsewhen(state === sha1topstate.length){
      write_enable := 1.U
      write_data := length << 3.U
    }.otherwise{
      write_enable := false.B
      write_data := 0.U
    }
    io.data_out := MuxCase(0.U, Array(
      (io.read_address === 0.U) -> sha1core.io.h0,
      (io.read_address === 1.U) -> sha1core.io.h1,
      (io.read_address === 2.U) -> sha1core.io.h2,
      (io.read_address === 3.U) -> sha1core.io.h3,
      (io.read_address === 4.U) -> sha1core.io.h4
    ))

  }
}

