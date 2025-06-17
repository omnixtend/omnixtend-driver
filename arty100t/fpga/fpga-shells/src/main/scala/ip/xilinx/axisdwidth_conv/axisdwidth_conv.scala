

package sifive.fpgashells.ip.xilinx.axisdwidth_conv


import chisel3._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util.ElaborationArtefacts
import org.chipsalliance.cde.config._



class AXISDWidthConv_64i_32o() extends BlackBox {
  override def desiredName = "axis_dwidth_converter_64i_32o"

    val io = IO(new Bundle {
      val aresetn		        = Input(Reset())
      val aclk				      = Input(Clock())
      val s_axis_tvalid			= Input(Bool())
      val s_axis_tready			= Output(Bool())
      val s_axis_tdata			= Input(UInt(64.W))
      val s_axis_tkeep			= Input(UInt(8.W))
      val s_axis_tlast			= Input(Bool())

      val m_axis_tvalid			= Output(Bool())
      val m_axis_tready			= Input(Bool())
      val m_axis_tdata			= Output(UInt(32.W))
      val m_axis_tkeep			= Output(UInt(4.W))
      val m_axis_tlast			= Output(Bool())
  })
  ElaborationArtefacts.add(s"axis_dwidth_converter_64i_32o.vivado.tcl",
    s"""create_ip -name axis_dwidth_converter -vendor xilinx.com -library ip -version 1.1 -module_name axis_dwidth_converter_64i_32o -dir $$ipdir -force
       |set_property -dict [list 							\\
       |  CONFIG.S_TDATA_NUM_BYTES {8}				\\
       |  CONFIG.M_TDATA_NUM_BYTES {4}		\\
       |  CONFIG.HAS_TKEEP {1}					\\
       |  CONFIG.HAS_TLAST {1}					\\
       |] [get_ips axis_dwidth_converter_64i_32o]
       |""".stripMargin)
}



class AXISDWidthConv_32i_64o() extends BlackBox {
  override def desiredName = "axis_dwidth_converter_32i_64o"

    val io = IO(new Bundle {
      val aresetn		        = Input(Reset())
      val aclk				      = Input(Clock())
      val s_axis_tvalid			= Input(Bool())
      val s_axis_tready			= Output(Bool())
      val s_axis_tdata			= Input(UInt(32.W))
      val s_axis_tkeep			= Input(UInt(4.W))
      val s_axis_tlast			= Input(Bool())

      val m_axis_tvalid			= Output(Bool())
      val m_axis_tready			= Input(Bool())
      val m_axis_tdata			= Output(UInt(64.W))
      val m_axis_tkeep			= Output(UInt(8.W))
      val m_axis_tlast			= Output(Bool())
  })
  ElaborationArtefacts.add(s"axis_dwidth_converter_32i_64o.vivado.tcl",
    s"""create_ip -name axis_dwidth_converter -vendor xilinx.com -library ip -version 1.1 -module_name axis_dwidth_converter_32i_64o -dir $$ipdir -force
       |set_property -dict [list 							\\
       |  CONFIG.S_TDATA_NUM_BYTES {4}				\\
       |  CONFIG.M_TDATA_NUM_BYTES {8}		\\
       |  CONFIG.HAS_TKEEP {1}					\\
       |  CONFIG.HAS_TLAST {1}					\\
       |] [get_ips axis_dwidth_converter_32i_64o]
       |""".stripMargin)
}