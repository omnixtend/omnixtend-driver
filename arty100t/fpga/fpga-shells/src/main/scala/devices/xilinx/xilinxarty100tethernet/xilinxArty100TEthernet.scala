package sifive.fpgashells.devices.xilinx.xilinxarty100tethernet

import chisel3._
import chisel3.experimental.attach
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.prci._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.tilelink._
import org.chipsalliance.cde.config.Parameters
import sifive.fpgashells.ip.xilinx.axi_ethernet._

import chisel3.util._ 
import freechips.rocketchip.util.ElaborationArtefacts
import sifive.fpgashells.clocks._

import chisel3.experimental.Analog

import omnixtend._
import sifive.fpgashells.ip.xilinx.vio._
import sifive.fpgashells.ip.xilinx.axisdatafifo._

import sifive.fpgashells.ip.xilinx.axisdwidth_conv._ 

import chisel3.experimental.{IntParam, BaseModule}
import freechips.rocketchip.regmapper.{HasRegMap, RegField}
import freechips.rocketchip.util.UIntIsOneOf




class AXISTXControlIO extends Bundle {
	val axis_resetn  					= Input(Bool())
	val axis_clk						= Input(Clock())

	val m_axis_txc_tdata     			= Output(UInt(32.W))
	val m_axis_txc_tkeep     			= Output(UInt(4.W))
	val m_axis_txc_tvalid    			= Output(Bool())
	val m_axis_txc_tlast     			= Output(Bool())
	val m_axis_txc_tready    			= Input(Bool())
	
	val m_axis_txd_tvalid     			= Input(Bool())
}

class AXISTXControlBlackBox extends BlackBox with HasBlackBoxResource{
	val io = IO(new AXISTXControlIO ())
	addResource("/vsrc/AXISTXControlBlackBox.v")
}

class XilinxArty100TEthernetIO extends AXIEthernetBlackBoxIO 
	with HasMDIO
	with HasArty100tBtn


case class XilinxArty100TEthernetParams (
  address : Seq[AddressSet]
) {

	def toAXIEthernetParams: AXIEthernetParams = AXIEthernetParams (
		name = "axi_ethernet_0", 
		speed = 1, 
		address = address 
	)
}

class XilinxArty100TEthernetPads extends AXIEthernetPads 
	with HasMDIO 
	with HasArty100tBtn {
  
  def this(c : XilinxArty100TEthernetParams) {
    this() 

	println(s"Ethernet controller name: ${c.toAXIEthernetParams.name}")
	println(s"Ethernet controller speed setting: ${c.toAXIEthernetParams.speed}")

  }
}

class XilinxArty100TEthernetIsland(c: XilinxArty100TEthernetParams)(implicit p: Parameters) extends LazyModule with CrossesToOnlyOneClockDomain {
  val crossing = SynchronousCrossing()
  val device = new SimpleDevice("ethernet", Seq("ucbbar,ethernet"))
  val ranges = AddressRange.fromSets(c.address)
  val node = AXI4SlaveNode(Seq(AXI4SlavePortParameters(
    slaves = Seq(AXI4SlaveParameters(
      address       = c.address,
      resources     = device.reg,
      regionType    = RegionType.UNCACHED,
      executable    = false,
      supportsWrite = TransferSizes(1, 4),
      supportsRead  = TransferSizes(1, 4))),
      beatBytes     = 4 )))

  lazy val module = new Impl
  class Impl extends LazyRawModuleImp(this) {
    val io = IO(new Bundle { val port = new XilinxArty100TEthernetIO()})
    override def provideImplicitClockToLazyChildren = true
    
    val blackbox = Module(new axi_ethernet(c.toAXIEthernetParams))
    
    val (axi_lite, _) = node.in(0)
    
	// Create IOBuf class, connect to mdio 
	class IOBUF extends BlackBox {
		val io = IO(new Bundle {
			val O = Output(Bool())  // Data from pad to fabric
			val IO = Analog(1.W)    // Inout pad
			val I = Input(Bool())   // Data from fabric to pad
			val T = Input(Bool())   // Tristate control (1 = input, 0 = output)
		})
	}


	val mdio_iobuf 				= Module(new IOBUF)

	mdio_iobuf.io.I	 		:= blackbox.io.mdio_mdio_o
	blackbox.io.mdio_mdio_i	:= mdio_iobuf.io.O	
	mdio_iobuf.io.T			:= blackbox.io.mdio_mdio_t

    io.port.mdio_mdc := blackbox.io.mdio_mdc
    attach(io.port.mdio, mdio_iobuf.io.IO)

    io.port.phy_rst_n := blackbox.io.phy_rst_n
    io.port.mii_txd := blackbox.io.mii_txd
    
	blackbox.io.mii_tx_clk := io.port.mii_tx_clk 
    
	io.port.mii_tx_en := blackbox.io.mii_tx_en
    io.port.mii_tx_er := blackbox.io.mii_tx_er
   
    blackbox.io.mii_rxd := io.port.mii_rxd
    blackbox.io.mii_rx_er := io.port.mii_rx_er 
    blackbox.io.mii_rx_dv := io.port.mii_rx_dv 
    blackbox.io.mii_rx_clk := io.port.mii_rx_clk

    // Connect clock signals
    // blackbox.io.ref_clk := io.port.ref_clk
    blackbox.io.gtx_clk := io.port.gtx_clk
    blackbox.io.axis_clk := io.port.axis_clk
    blackbox.io.s_axi_lite_clk := io.port.s_axi_lite_clk
    blackbox.io.s_axi_lite_resetn := io.port.s_axi_lite_resetn

    // Connect AXI-Stream signals
    // TX Data
    blackbox.io.s_axis_txd_tdata		:= io.port.s_axis_txd_tdata
    blackbox.io.s_axis_txd_tkeep		:= io.port.s_axis_txd_tkeep
    blackbox.io.s_axis_txd_tlast		:= io.port.s_axis_txd_tlast
    blackbox.io.s_axis_txd_tvalid		:= io.port.s_axis_txd_tvalid
    io.port.s_axis_txd_tready			:= blackbox.io.s_axis_txd_tready

    // // TX Control
    blackbox.io.s_axis_txc_tdata		:= io.port.s_axis_txc_tdata
    blackbox.io.s_axis_txc_tkeep		:= io.port.s_axis_txc_tkeep
    blackbox.io.s_axis_txc_tlast		:= io.port.s_axis_txc_tlast
    blackbox.io.s_axis_txc_tvalid 		:= io.port.s_axis_txc_tvalid
    io.port.s_axis_txc_tready 			:= blackbox.io.s_axis_txc_tready

    // // RX Data
    io.port.m_axis_rxd_tdata			:= blackbox.io.m_axis_rxd_tdata
    io.port.m_axis_rxd_tkeep			:= blackbox.io.m_axis_rxd_tkeep
    io.port.m_axis_rxd_tlast			:= blackbox.io.m_axis_rxd_tlast
    io.port.m_axis_rxd_tvalid			:= blackbox.io.m_axis_rxd_tvalid
    blackbox.io.m_axis_rxd_tready		:= io.port.m_axis_rxd_tready

    // // RX Status
    io.port.m_axis_rxs_tdata			:= blackbox.io.m_axis_rxs_tdata
    io.port.m_axis_rxs_tkeep			:= blackbox.io.m_axis_rxs_tkeep
    io.port.m_axis_rxs_tlast			:= blackbox.io.m_axis_rxs_tlast
    io.port.m_axis_rxs_tvalid			:= blackbox.io.m_axis_rxs_tvalid
    blackbox.io.m_axis_rxs_tready		:= true.B 


    // Connect AXI4-Lite interface
    // AR channel (read address)
    blackbox.io.s_axi_araddr := axi_lite.ar.bits.addr
    blackbox.io.s_axi_arvalid := axi_lite.ar.valid
    axi_lite.ar.ready := blackbox.io.s_axi_arready
    
    // AW channel (write address)
    blackbox.io.s_axi_awaddr := axi_lite.aw.bits.addr
    blackbox.io.s_axi_awvalid := axi_lite.aw.valid
    axi_lite.aw.ready := blackbox.io.s_axi_awready
    
    // R channel (read data)
    axi_lite.r.bits.data := blackbox.io.s_axi_rdata
    axi_lite.r.bits.resp := blackbox.io.s_axi_rresp
    axi_lite.r.valid := blackbox.io.s_axi_rvalid
    blackbox.io.s_axi_rready := axi_lite.r.ready
    
    // W channel (write data)
    blackbox.io.s_axi_wdata := axi_lite.w.bits.data
    blackbox.io.s_axi_wstrb := axi_lite.w.bits.strb
    blackbox.io.s_axi_wvalid := axi_lite.w.valid
    axi_lite.w.ready := blackbox.io.s_axi_wready
    
    // B channel (write response)
    axi_lite.b.bits.resp := blackbox.io.s_axi_bresp
    axi_lite.b.valid := blackbox.io.s_axi_bvalid
    blackbox.io.s_axi_bready := axi_lite.b.ready

    // Connect reset signals
    blackbox.io.axi_txd_arstn := io.port.axi_txd_arstn
    blackbox.io.axi_txc_arstn := io.port.axi_txc_arstn
    blackbox.io.axi_rxd_arstn := io.port.axi_rxd_arstn
    blackbox.io.axi_rxs_arstn := io.port.axi_rxs_arstn


    // clock and reset signals
    blackbox.io.axis_clk				:= io.port.axis_clk
    blackbox.io.axi_txd_arstn			:= io.port.axi_txd_arstn
    blackbox.io.axi_txc_arstn			:= io.port.axi_txc_arstn
    blackbox.io.axi_rxd_arstn			:= io.port.axi_rxd_arstn
    blackbox.io.axi_rxs_arstn			:= io.port.axi_rxs_arstn

    io.port.mac_irq						:= blackbox.io.mac_irq
    io.port.interrupt					:= blackbox.io.interrupt


  }
}

class AXIEthSysTop(c: XilinxArty100TEthernetParams, crossing: ClockCrossingType = AsynchronousCrossing(8))
  (implicit p: Parameters) extends LazyModule {
	val ranges					= AddressRange.fromSets(c.address)
	val depth					= ranges.head.size

	val buffer					= LazyModule(new TLBuffer)
	val toaxi4					= LazyModule(new TLToAXI4(adapterName = Some("mem")))
	val indexer					= LazyModule(new AXI4IdIndexer(idBits = 4))
	val deint					= LazyModule(new AXI4Deinterleaver(p(CacheBlockBytes)))
	val yank					= LazyModule(new AXI4UserYanker)
	val island					= LazyModule(new XilinxArty100TEthernetIsland(c))
	
	val ox						= LazyModule(new OmniXtendNode()(p))

  	val node: TLInwardNode =
    island.crossAXI4In(island.node) := yank.node := deint.node := indexer.node := toaxi4.node := buffer.node

    // Connecting TileLink nodes
  	val node2: TLInwardNode =
    ox.node := TLBuffer() := TLWidthWidget(8) := TLXbar()

  	lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val ox2ethfifo	= Module(new AXI4StreamFIFO())
	val dconvox2eth = Module(new AXISDWidthConv_64i_32o())

    val eth2oxfifo	= Module(new AXI4StreamFIFO())
	val dconveth2ox = Module(new AXISDWidthConv_32i_64o())

	val axistxctrl	= Module(new AXISTXControlBlackBox())

	val vio 		= Module(new VIO())

    val io 			= IO(new Bundle { val port = new XilinxArty100TEthernetIO() })

	io.port			<> island.module.io.port

	////////////////////////////////////////////////////////////////////////////
	//  TX - AXIS DATA CONNECTION 
	////////////////////////////////////////////////////////////////////////////

	//CLK, RESET
	dconvox2eth.io.aclk						:= island.module.io.port.axis_clk
	dconvox2eth.io.aresetn					:= !Module.reset.asBool

	ox2ethfifo.io.s_axis_aresetn			:= !Module.reset.asBool
	ox2ethfifo.io.s_axis_aclk				:= Module.clock
	ox2ethfifo.io.m_axis_aclk				:= island.module.io.port.axis_clk
	////////

	//OX TO FIFO (TX)
	ox2ethfifo.io.s_axis_tvalid				:= ox.module.io.txvalid
	ox2ethfifo.io.s_axis_tdata				:= ox.module.io.txdata
	ox2ethfifo.io.s_axis_tlast				:= ox.module.io.txlast
	ox2ethfifo.io.s_axis_tkeep				:= ox.module.io.txkeep
	ox.module.io.txready					:= ox2ethfifo.io.s_axis_tready

	// FIFO TO DWIDTH CONV (TX)
	dconvox2eth.io.s_axis_tvalid			:= ox2ethfifo.io.m_axis_tvalid
	dconvox2eth.io.s_axis_tdata				:= ox2ethfifo.io.m_axis_tdata
	dconvox2eth.io.s_axis_tlast				:= ox2ethfifo.io.m_axis_tlast
	dconvox2eth.io.s_axis_tkeep				:= ox2ethfifo.io.m_axis_tkeep
	ox2ethfifo.io.m_axis_tready				:= dconvox2eth.io.s_axis_tready

	// DWIDTH CONV TO ETH (TX)
	island.module.io.port.s_axis_txd_tvalid	:= dconvox2eth.io.m_axis_tvalid
	island.module.io.port.s_axis_txd_tdata	:= dconvox2eth.io.m_axis_tdata
	island.module.io.port.s_axis_txd_tlast	:= dconvox2eth.io.m_axis_tlast
	island.module.io.port.s_axis_txd_tkeep	:= dconvox2eth.io.m_axis_tkeep
	dconvox2eth.io.m_axis_tready			:= island.module.io.port.s_axis_txd_tready


	////////////////////////////////////////////////////////////////////////////

	////////////////////////////////////////////////////////////////////////////
	//  RX - AXIS CONNECTION 
	////////////////////////////////////////////////////////////////////////////

	//CLK, RESET
	eth2oxfifo.io.s_axis_aresetn			:= !Module.reset.asBool
	eth2oxfifo.io.s_axis_aclk				:= island.module.io.port.axis_clk
	eth2oxfifo.io.m_axis_aclk				:= Module.clock

	dconveth2ox.io.aclk						:= island.module.io.port.axis_clk
	dconveth2ox.io.aresetn					:= !Module.reset.asBool

	//ETH TO DWIDTH CONV
	dconveth2ox.io.s_axis_tvalid			:= island.module.io.port.m_axis_rxd_tvalid
	dconveth2ox.io.s_axis_tdata				:= island.module.io.port.m_axis_rxd_tdata
	dconveth2ox.io.s_axis_tlast				:= island.module.io.port.m_axis_rxd_tlast
	dconveth2ox.io.s_axis_tkeep				:= island.module.io.port.m_axis_rxd_tkeep
	island.module.io.port.m_axis_rxd_tready := dconveth2ox.io.s_axis_tready

	//DWIDTH CONV TO FIFO
	eth2oxfifo.io.s_axis_tvalid				:= dconveth2ox.io.m_axis_tvalid
	eth2oxfifo.io.s_axis_tdata				:= dconveth2ox.io.m_axis_tdata
	eth2oxfifo.io.s_axis_tlast				:= dconveth2ox.io.m_axis_tlast
	eth2oxfifo.io.s_axis_tkeep				:= dconveth2ox.io.m_axis_tkeep
	dconveth2ox.io.m_axis_tready			:= eth2oxfifo.io.s_axis_tready

	//FIFO TO OX
	ox.module.io.rxdata						:= eth2oxfifo.io.m_axis_tdata
	ox.module.io.rxvalid					:= eth2oxfifo.io.m_axis_tvalid
	ox.module.io.rxlast						:= eth2oxfifo.io.m_axis_tlast
	eth2oxfifo.io.m_axis_tready				:= true.B

	////////////////////////////////////////////////////////////////////////////
	//  TX - AXIS CONTROL CONNECTION 
	////////////////////////////////////////////////////////////////////////////

	axistxctrl.io.axis_clk					:= island.module.io.port.axis_clk
	axistxctrl.io.axis_resetn				:= !Module.reset.asBool

	island.module.io.port.s_axis_txc_tdata 	:= axistxctrl.io.m_axis_txc_tdata
	island.module.io.port.s_axis_txc_tkeep 	:= axistxctrl.io.m_axis_txc_tkeep
	island.module.io.port.s_axis_txc_tvalid := axistxctrl.io.m_axis_txc_tvalid
	island.module.io.port.s_axis_txc_tlast 	:= axistxctrl.io.m_axis_txc_tlast
	axistxctrl.io.m_axis_txc_tready			:= island.module.io.port.s_axis_txc_tready
	axistxctrl.io.m_axis_txd_tvalid			:= dconvox2eth.io.m_axis_tvalid
	////////////////////////////////////////////////////////////////////////////


		val vio_reg0				= RegInit(false.B)
		val vio_reg1				= RegInit(false.B)
		val vio_reg2				= RegInit(false.B)
		val vio_reg3				= RegInit(false.B)
		val ox_open_vio				= RegInit(false.B)
		val ox_open_btn				= RegInit(false.B)
		val ox_close_vio			= RegInit(false.B)
		val ox_close_btn			= RegInit(false.B)
		val debug1_vio				= RegInit(false.B)
		val debug1_btn				= RegInit(false.B)
		val debug2_vio				= RegInit(false.B)
		val debug2_btn				= RegInit(false.B)
		val vio_test_reg			= RegInit(0.U(32.W))
		val debug1					= RegInit(false.B)
		val debug2					= RegInit(false.B)


		val idle0 :: wait0 :: Nil	= Seq(0, 1).map(_.U)
		val state0					= RegInit(idle0)
		val filt_cnt0				= RegInit(0.U(32.W))
		val gpio_reg0				= RegInit(false.B)

		//vio
		vio.io.clk								:= Module.clock
		vio_reg0								:= vio.io.probe_out0
		vio_reg1								:= vio.io.probe_out1
		vio_reg2								:= vio.io.probe_out2
		vio_reg3								:= vio.io.probe_out3
		ox_open_vio								:= vio.io.probe_out0 && !vio_reg0
		ox_close_vio							:= vio.io.probe_out1 && !vio_reg1
		debug1_vio								:= vio.io.probe_out2 && !vio_reg2
		debug2_vio								:= vio.io.probe_out3 && !vio_reg3

		//ox_open
		gpio_reg0 := io.port.btn0
		when(state0 === idle0){
		when(io.port.btn0){
			ox_open_btn := io.port.btn0 && !gpio_reg0
			state0 := wait0
		}
		}.elsewhen(state0 === wait0){
		ox_open_btn := false.B
		when(filt_cnt0 < 100000000.U){// wait 1s
			filt_cnt0 := filt_cnt0 + 1.U
		}.otherwise{
			filt_cnt0 := 0.U
			state0 := idle0
		}
		}
		//ox_close
		val idle1 :: wait1 :: Nil = Seq(0, 1).map(_.U)

		val state1 = RegInit(idle1)
		val filt_cnt1 = RegInit(0.U(32.W))
		val gpio_reg1 = RegInit(false.B)
		gpio_reg1 := io.port.btn1
		when(state1 === idle1){
		when(io.port.btn1){
			ox_close_btn := io.port.btn1 && !gpio_reg1
			state1 := wait1
		}
		}.elsewhen(state1 === wait1){
		ox_close_btn := false.B
		when(filt_cnt1 < 100000000.U){// wait 1s
			filt_cnt1 := filt_cnt1 + 1.U
		}.otherwise{
			filt_cnt1 := 0.U
			state1 := idle1
		}
		}
		//debug1
		val idle2 :: wait2 :: Nil = Seq(0, 1).map(_.U)

		val state2 = RegInit(idle2)
		val filt_cnt2 = RegInit(0.U(32.W))
		val gpio_reg2 = RegInit(false.B)
		gpio_reg2 := io.port.btn2
		when(state2 === idle2){
		when(io.port.btn2){
			debug1_btn := io.port.btn2 && !gpio_reg2
			state2 := wait2
		}
		}.elsewhen(state2 === wait2){
		debug1_btn := false.B
		when(filt_cnt2 < 100000000.U){// wait 1s
			filt_cnt2 := filt_cnt2 + 1.U
		}.otherwise{
			filt_cnt2 := 0.U
			state2 := idle2
		}
		}
		//debug2
		val idle3 :: wait3 :: Nil = Seq(0, 1).map(_.U)

		val state3 = RegInit(idle3)
		val filt_cnt3 = RegInit(0.U(32.W))
		val gpio_reg3 = RegInit(false.B)
		gpio_reg3 := io.port.btn3
		when(state3 === idle3){
		when(io.port.btn3){
			debug2_btn := io.port.btn3 && !gpio_reg3
			state3 := wait3
		}
		}.elsewhen(state3 === wait3){
		debug2_btn := false.B
		when(filt_cnt3 < 100000000.U){// wait 1s
			filt_cnt3 := filt_cnt3 + 1.U
		}.otherwise{
			filt_cnt3 := 0.U
			state3 := idle3
		}
		}
		// Connecting VIO and GPIO signals to the OX core
		ox.module.io.ox_open			:= ox_open_vio || ox_open_btn
		ox.module.io.ox_close			:= ox_close_vio || ox_close_btn
		ox.module.io.debug1				:= debug1_vio || debug1_btn
		ox.module.io.debug2				:= debug2_vio || debug2_btn
//*/
  }
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
