package omnixtend

import chisel3._
import chisel3.util._

class PacketGenerator extends Module {
  val io = IO(new Bundle {
    val test_mode = Input(Bool())
    val m_axis_tready = Input(Bool())
    val rx_axis_tvalid = Input(Bool())
    val m_axis_tvalid = Output(Bool())
    val m_axis_tdata = Output(UInt(512.W))
    val m_axis_tlast = Output(Bool())
    val m_axis_tkeep = Output(UInt(64.W))
    val o_thr_cnt = Output(UInt(14.W))
    val o_rx_cnt = Output(UInt(14.W))
    val o_thr_valid = Output(Bool())
    val o_rx_valid = Output(Bool())
  })
  val r_axis_tvalid = RegInit(false.B)
  val r_axis_tdata = RegInit(0.U(512.W))
  val r_axis_tlast = RegInit(false.B)
  val r_axis_tkeep = RegInit(0.U(64.W))
  val r_pkt_cnt = RegInit(0.U(10.W))
  val tic_cnt = RegInit(0.U(14.W))
  val thr_cnt = RegInit(0.U(14.W))
  val thr_cnt_r = RegInit(0.U(14.W))
  val rx_cnt = RegInit(0.U(14.W))
  val rx_cnt_r = RegInit(0.U(14.W))
  val r_thr_valid = RegInit(false.B)
  val r_rx_valid = RegInit(false.B)

  io.m_axis_tvalid := r_axis_tvalid
  io.m_axis_tdata := r_axis_tdata
  io.m_axis_tlast := r_axis_tlast
  io.m_axis_tkeep := r_axis_tkeep
  io.o_thr_cnt := thr_cnt_r
  io.o_rx_cnt := rx_cnt_r
  io.o_thr_valid := r_thr_valid
  io.o_rx_valid := r_rx_valid

  val keep32 = "hFFFFFFFF".U(32.W)
  r_axis_tkeep := (keep32 << 32) | keep32

  when(io.test_mode){
    when(r_pkt_cnt < 100.U){
        when(io.m_axis_tready){
            r_axis_tvalid := true.B
            r_pkt_cnt := r_pkt_cnt + 1.U
            when(r_pkt_cnt === 99.U){
                r_axis_tlast := true.B
            }
        }.otherwise{
            r_axis_tvalid := false.B
            r_axis_tlast := false.B
        }
    }.otherwise{
        r_pkt_cnt := 0.U
        r_axis_tvalid := false.B
        r_axis_tlast := false.B
    }
  }.otherwise{
    r_axis_tvalid := false.B
    r_axis_tdata := 0.U
    r_axis_tlast:= false.B
    r_pkt_cnt := 0.U
  }

  when(tic_cnt < 9999.U){
    r_thr_valid := false.B
    tic_cnt := tic_cnt + 1.U
    when(r_axis_tvalid && io.m_axis_tready){
        thr_cnt := thr_cnt + 1.U
    }
  }.elsewhen(tic_cnt === 9999.U){
    tic_cnt := 0.U
    thr_cnt_r := thr_cnt
    thr_cnt := 0.U
    r_thr_valid := true.B
  }

  when(tic_cnt < 9999.U){
    r_rx_valid := false.B
    when(io.rx_axis_tvalid){
        rx_cnt := rx_cnt + 1.U
    }
  }.elsewhen(tic_cnt === 9999.U){
    rx_cnt_r := rx_cnt
    rx_cnt := 0.U
    r_rx_valid := true.B
  }
}