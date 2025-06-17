

package sifive.fpgashells.ip.xilinx.axisdatafifo


import chisel3._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util.ElaborationArtefacts
import org.chipsalliance.cde.config._



class AXI4StreamFIFO() extends BlackBox { // Xilinx AXI4-Stream FIFO IP for clock domain crossing between 100MHz and 156.25MHz
  override def desiredName = "axis_data_fifo_0"

  val io = IO(new Bundle {
    val s_axis_aresetn			= Input(Reset())
    val s_axis_aclk				= Input(Clock())
    val s_axis_tvalid			= Input(Bool())
    val s_axis_tready			= Output(Bool())
    val s_axis_tdata			= Input(UInt(64.W))
    val s_axis_tkeep			= Input(UInt(8.W))
    val s_axis_tlast			= Input(Bool())

    val m_axis_aclk				= Input(Clock())
    val m_axis_tvalid			= Output(Bool())
    val m_axis_tready			= Input(Bool())
    val m_axis_tdata			= Output(UInt(64.W))
    val m_axis_tkeep			= Output(UInt(8.W))
    val m_axis_tlast			= Output(Bool())
  })
  ElaborationArtefacts.add(s"axis_data_fifo_0.vivado.tcl",
    s"""create_ip -name axis_data_fifo -vendor xilinx.com -library ip -version 2.0 -module_name axis_data_fifo_0 -dir $$ipdir -force
       |set_property -dict [list 							\\
       |  CONFIG.TDATA_NUM_BYTES {8}		\\
       |  CONFIG.FIFO_MODE {2}				\\
       |  CONFIG.IS_ACLK_ASYNC {1}				\\
       |  CONFIG.HAS_TKEEP {1}					\\
       |  CONFIG.HAS_TLAST {1}					\\
       |] [get_ips axis_data_fifo_0]
       |""".stripMargin)
}