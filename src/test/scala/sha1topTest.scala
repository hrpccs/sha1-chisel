package sha1

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.security.MessageDigest

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

object testbenchState extends ChiselEnum {
  val idle,readingLength,readingData,restartSha1,sha1Running,sha1waiting,sha1Finished,testDone = Value
}

class testbench extends Module {
  val io = IO(new Bundle{
    val datain = Input(UInt(32.W))
    val length = Input(UInt(32.W))
    val actualLength = Input(UInt(32.W))
    val start = Input(Bool())
    val restart = Input(Bool())
    val h0 = Output(UInt(32.W))
    val h1 = Output(UInt(32.W))
    val h2 = Output(UInt(32.W))
    val h3 = Output(UInt(32.W))
    val h4 = Output(UInt(32.W))
    val valid = Output(Bool())
  })

  val sha1top = Module(new sha1top)

  val mem = Reg(Vec(1024,UInt(32.W)))
  val memaddr = RegInit(0.U(10.W))
  val length = RegInit(0.U(32.W))
  val actualLength = RegInit(0.U(32.W))
  val state = RegInit(testbenchState.idle)
  val currTestLen = RegInit(0.U(32.W))
  val stringIndex = RegInit(0.U(32.W))
  val readIndex = RegInit(0.U(32.W))
  val valid = RegInit(false.B)

  val h0 = RegInit(0.U(32.W))
  val h1 = RegInit(0.U(32.W))
  val h2 = RegInit(0.U(32.W))
  val h3 = RegInit(0.U(32.W))
  val h4 = RegInit(0.U(32.W))

  io.h0 := h0
  io.h1 := h1
  io.h2 := h2
  io.h3 := h3
  io.h4 := h4

  sha1top.io.nreset := true.B
  sha1top.io.write_enable := false.B
  sha1top.io.last_len := 0.U
  sha1top.io.last := false.B
  sha1top.io.data_in := 0.U
  sha1top.io.read_address := readIndex

  io.valid := valid

  when(state === testbenchState.idle){
    when(io.start){
      state := testbenchState.readingLength
    }
  }.elsewhen(state === testbenchState.readingLength){
    length := io.length
    actualLength := io.actualLength
    state := testbenchState.readingData
  }.elsewhen(state === testbenchState.readingData){

    when(memaddr === length){
      state := testbenchState.restartSha1
    }.elsewhen(memaddr < length){
      mem(memaddr) := io.datain
      memaddr := memaddr + 1.U
    }
  }.elsewhen(state === testbenchState.restartSha1){
    sha1top.io.nreset := false.B
    state := testbenchState.sha1Running
  }.elsewhen(state === testbenchState.sha1Running){
    when(!sha1top.io.busy){
      when(stringIndex + 4.U <= currTestLen) {
        sha1top.io.write_enable := true.B
        sha1top.io.last := false.B
        sha1top.io.last_len := 0.U
        sha1top.io.data_in := mem(stringIndex / 4.U)
        stringIndex := stringIndex + 4.U
      }.otherwise{
        sha1top.io.write_enable := true.B
        sha1top.io.last := true.B
        sha1top.io.last_len := (currTestLen % 4.U)
        sha1top.io.data_in := mem(stringIndex / 4.U)
        stringIndex := stringIndex + 4.U
        state := testbenchState.sha1waiting
      }
    }
  }.elsewhen(state === testbenchState.sha1waiting){
    when(sha1top.io.ready){
      readIndex := 0.U
      state := testbenchState.sha1Finished
    }
  }.elsewhen(state === testbenchState.sha1Finished){
    when(readIndex < 5.U){
      when(readIndex === 0.U){
        h0 := sha1top.io.data_out
      }.elsewhen(readIndex === 1.U){
        h1 := sha1top.io.data_out
      }.elsewhen(readIndex === 2.U){
        h2 := sha1top.io.data_out
      }.elsewhen(readIndex === 3.U){
        h3 := sha1top.io.data_out
      }.elsewhen(readIndex === 4.U){
        h4 := sha1top.io.data_out
      }
      readIndex := readIndex + 1.U
    }.otherwise{
      valid := true.B
      state := testbenchState.testDone
    }
  }.elsewhen(state === testbenchState.testDone){
    when(io.restart){
      when(currTestLen < actualLength){
        valid := false.B
        currTestLen := currTestLen + 1.U
        stringIndex := 0.U
        state := testbenchState.restartSha1
      }
    }
  }
}



class sha1topTest extends AnyFlatSpec with ChiselScalatestTester {

  def sha1(input: String):String = {
    val md = MessageDigest.getInstance("SHA-1")
    md.digest(input.getBytes(StandardCharsets.US_ASCII)).map("%02x".format(_)).mkString
  }
  behavior of "sha1top"
  it should "work" in {
    // simulate a situation that c.io.in is 0x61626380L, then 0x0, 0x0 and so on
    // use a vector to store the time seq value of c.io.in
    //
    val str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
//    val str = "abc"
    val actuallength = str.length
    val paddedString = str.padTo(str.length + (4 - str.length % 4), '\u0000')
    val stringLength = paddedString.length
    val byteBuffer = ByteBuffer.allocate(stringLength).put(paddedString.getBytes(StandardCharsets.US_ASCII))
    byteBuffer.flip()
    val inputArray = Array.fill(stringLength / 4)(byteBuffer.getInt())
//    println(inputArray.map(_.toHexString).mkString(",")

    test(new testbench).withAnnotations(TestAnnotations.annos) { c =>
      c.io.restart.poke(false.B)
      c.io.start.poke(true.B)
      c.clock.step()
      c.io.start.poke(false.B)
      c.io.length.poke((stringLength / 4).U)
      c.io.actualLength.poke(actuallength.U)
      c.clock.step()
      for(i <- 0 until stringLength / 4){
        c.io.datain.poke(inputArray(i).U)
        c.clock.step()
      }
      println(String.format("digest:%s",str))
      println(String.format("substr's length\t                SHA1            \t           standardsha1                         \tequal?", str))
      for(i <- 0 until actuallength){
        c.io.restart.poke(false.B)
        c.clock.step(110 * (1+ i/50) + i)
        c.io.valid.expect(true.B)
        val h0 = c.io.h0.peek().litValue.toLong
        val h1 = c.io.h1.peek().litValue.toLong
        val h2 = c.io.h2.peek().litValue.toLong
        val h3 = c.io.h3.peek().litValue.toLong
        val h4 = c.io.h4.peek().litValue.toLong
        val myoutput = String.format("%08x%08x%08x%08x%08x",h0,h1,h2,h3,h4)
        val ans = sha1(str.substring(0,i))
        println(String.format("%12d\t%s\t%s\t%s",i,myoutput,ans,myoutput.equals(ans)))
//        println(String.format("%4d\t%08x%08x%08x%08x%08x\t%s\t", i,h0,h1,h2,h3,h4))
        // h0 - h4 must print as hex
//        println(s"hash str:$str, SHA1: ${h0.toHexString}${h1.toHexString}${h2.toHexString}${h3.toHexString}${h4.toHexString}")
        c.io.restart.poke(true.B)
        c.clock.step()
      }

    }
  }
}
