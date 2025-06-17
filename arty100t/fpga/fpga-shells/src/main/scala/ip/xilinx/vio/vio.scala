package sifive.fpgashells.ip.xilinx.vio


import chisel3._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util.ElaborationArtefacts
import org.chipsalliance.cde.config._



class VIO() extends BlackBox { // Xilinx VIO IP configured for use with the OX Core
  override def desiredName = "vio_0"

  val io = IO(new Bundle{
    val clk = Input(Clock())
    val probe_out0 = Output(Bool())
    val probe_out1 = Output(Bool())
    val probe_out2 = Output(Bool())
    val probe_out3 = Output(Bool())
  })
  ElaborationArtefacts.add(s"vio_0.vivado.tcl",
    s"""create_ip -name vio -vendor xilinx.com -library ip -version 3.0 -module_name vio_0 -dir $$ipdir -force
       |set_property -dict [list 							\\
       |  CONFIG.C_NUM_PROBE_OUT {4}		\\
       |  CONFIG.C_EN_PROBE_IN_ACTIVITY {0}		\\
       |  CONFIG.C_NUM_PROBE_IN {0}				\\
       |] [get_ips vio_0]
       |""".stripMargin)
}