package sifive.fpgashells.ip.xilinx.axi_ethernet

import chisel3._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util.ElaborationArtefacts
import org.chipsalliance.cde.config._

import freechips.rocketchip.prci.{SynchronousCrossing, AsynchronousCrossing, RationalCrossing}

import sifive.blocks.util._
import freechips.rocketchip.amba._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.tilelink._
import omnixtend._
import org.chipsalliance.diplomacy._
import freechips.rocketchip.amba.axi4._

import chisel3.util._


import chisel3.experimental.Analog

trait HasAXIEthernetExtIO extends Bundle{

// RGMII 
  val mii_txd		= Output(UInt(4.W))
  val mii_tx_clk		= Input(Bool()) 
  val mii_tx_en	= Output(Bool()) 

  val mii_rxd		= Input(UInt(4.W))
  val mii_rx_er		= Input(Bool())
  val mii_rx_dv		= Input(Bool()) 
  val mii_rx_clk		= Input(Bool()) 
}



trait HasBlackBoxIO extends Bundle{
// RGMII 
  val mii_txd		= Output(UInt(4.W))
  val mii_tx_clk		= Input(Bool()) 
  val mii_tx_en	= Output(Bool()) 

  val mii_rxd		= Input(UInt(4.W))
  val mii_rx_er		= Input(Bool())
  val mii_rx_dv		= Input(Bool()) 
  val mii_rx_clk		= Input(Bool()) 

}

trait HasMDIO extends Bundle{
  val mdio		= Analog(1.W)
  val mdio_mdc	= Output(Bool())
}

trait HasPHYResetOut extends Bundle{
  val phy_rst_n		= Output(Bool())
}


trait HasAXIEthernetClocksResets extends Bundle{
  val gtx_clk		          = Input(Clock())
  val axis_clk		        = Input(Clock())
  val axi_txd_arstn	      = Input(Bool())
  val axi_txc_arstn	      = Input(Bool())
  val axi_rxd_arstn	      = Input(Bool())
  val axi_rxs_arstn	      = Input(Bool())
  val s_axi_lite_clk		  = Input(Clock())
  val s_axi_lite_resetn		= Input(Bool())
}

trait HasAXIEthernetMAC extends Bundle {
  // AXI4-Stream TX Data
  val s_axis_txd_tdata		= Input(UInt(32.W))
  val s_axis_txd_tkeep		= Input(UInt(4.W))
  val s_axis_txd_tlast		= Input(Bool())
  val s_axis_txd_tready		= Output(Bool())
  val s_axis_txd_tvalid		= Input(Bool())

  // AXI4-Stream TX Control
  val s_axis_txc_tdata		= Input(UInt(32.W))
  val s_axis_txc_tkeep		= Input(UInt(4.W))
  val s_axis_txc_tlast		= Input(Bool())
  val s_axis_txc_tready		= Output(Bool())
  val s_axis_txc_tvalid		= Input(Bool())

  // AXI4-Stream RX Data
  val m_axis_rxd_tdata		= Output(UInt(32.W))
  val m_axis_rxd_tkeep		= Output(UInt(4.W))
  val m_axis_rxd_tlast		= Output(Bool())
  val m_axis_rxd_tready		= Input(Bool())
  val m_axis_rxd_tvalid		= Output(Bool())

  // AXI4-Stream RX Control
  val m_axis_rxs_tdata		= Output(UInt(32.W))
  val m_axis_rxs_tkeep		= Output(UInt(4.W))
  val m_axis_rxs_tlast		= Output(Bool())
  val m_axis_rxs_tready		= Input(Bool())
  val m_axis_rxs_tvalid		= Output(Bool())
  
}

trait HasArty100tBtn extends Bundle{
	val btn0				= Input(Bool())
	val btn1				= Input(Bool())
	val btn2				= Input(Bool())
	val btn3				= Input(Bool())
}


trait HasAXIEthernetJunk extends Bundle {
	val mac_irq			= Output(Bool())
	val interrupt			= Output(Bool())
  val mii_tx_er	= Output(Bool()) 
  val phy_rst_n   = Output(Bool())
}

trait HasAXIEthernetAXI4Lite extends Bundle { // AXI4-Lite I/O


  val s_axi_araddr			= Input(UInt(18.W))
  val s_axi_arvalid			= Input(Bool())
  val s_axi_arready			= Output(Bool())
 
  val s_axi_awaddr			= Input(UInt(18.W))
  val s_axi_awvalid			= Input(Bool())
  val s_axi_awready			= Output(Bool())

  val s_axi_rdata			  = Output(UInt(32.W))
  val s_axi_rresp			  = Output(UInt(2.W))
  val s_axi_rvalid			= Output(Bool())
  val s_axi_rready			= Input(Bool())

  val s_axi_wdata			  = Input(UInt(32.W))
  val s_axi_wstrb			  = Input(UInt(4.W))
  val s_axi_wvalid			= Input(Bool())
  val s_axi_wready			= Output(Bool())

  val s_axi_bresp			  = Output(UInt(2.W))
  val s_axi_bvalid			= Output(Bool())
  val s_axi_bready			= Input(Bool())

}

class AXIEthernetBlackBoxIO extends Bundle
  with HasBlackBoxIO
  with HasAXIEthernetClocksResets
  with HasAXIEthernetMAC
  with HasAXIEthernetJunk

class AXIEthernetPads() extends Bundle with HasAXIEthernetExtIO
class AXIEthernetMAC() extends Bundle with HasAXIEthernetMAC
class AXIEthernetClocks() extends Bundle with HasAXIEthernetClocksResets
class AXIEthernetAXI4Lite() extends Bundle with HasAXIEthernetAXI4Lite

trait AXIEthernetParamsBase

case class AXIEthernetParams(
  name			: String,
  speed			: Int,
  address		: Seq[AddressSet]
)
{
  val gtx_clkMHz	= 100
}

class axi_ethernet(c: AXIEthernetParams) extends BlackBox
{
  override def desiredName = c.name

  val io = IO(new AXIEthernetBlackBoxIO {
    val s_axi_araddr			= Input(UInt(18.W))
    val s_axi_arvalid			= Input(Bool())
    val s_axi_arready			= Output(Bool())
  
    val s_axi_awaddr			= Input(UInt(18.W))
    val s_axi_awvalid			= Input(Bool())
    val s_axi_awready			= Output(Bool())

    val s_axi_rdata				= Output(UInt(32.W))
    val s_axi_rresp				= Output(UInt(2.W))
    val s_axi_rvalid			= Output(Bool())
    val s_axi_rready			= Input(Bool())

    val s_axi_wdata				= Input(UInt(32.W))
    val s_axi_wstrb				= Input(UInt(4.W))
    val s_axi_wvalid			= Input(Bool())
    val s_axi_wready			= Output(Bool())

    val s_axi_bresp				= Output(UInt(2.W))
    val s_axi_bvalid			= Output(Bool())
    val s_axi_bready			= Input(Bool())


    val mdio_mdc         = Output(Bool())
    val mdio_mdio_i      = Input(Bool())
    val mdio_mdio_o      = Output(Bool())
    val mdio_mdio_t      = Output(Bool())

  })


  ElaborationArtefacts.add(s"${desiredName}.vivado.tcl",
    s"""create_ip -name axi_ethernet -vendor xilinx.com -library ip -version 7.2 -module_name ${desiredName} -dir $$ipdir -force
		| set_property -dict [list \\
    | CONFIG.PHY_TYPE {MII} \\
		| CONFIG.axiliteclkrate {100.0} \\
  	| CONFIG.axisclkrate {100.0} \\
		| ] [get_ips ${desiredName}]
       |""".stripMargin)

}


/*
   Copyright 2016 SiFive, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
