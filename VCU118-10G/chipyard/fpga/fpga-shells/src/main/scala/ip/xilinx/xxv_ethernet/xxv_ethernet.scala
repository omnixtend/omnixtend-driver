package sifive.fpgashells.ip.xilinx.xxv_ethernet

import chisel3._
import chisel3.util._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.util.{ElaborationArtefacts}
import sifive.blocks.util._
import freechips.rocketchip.amba._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.tilelink._
import omnixtend._

trait HasXXVEthernetPads {
  val gt_txp_out_0 = Output(Bool())
  val gt_txn_out_0 = Output(Bool())
  val gt_rxp_in_0 = Input(Bool())
  val gt_rxn_in_0 = Input(Bool())
  val gt_refclk_p = Input(Clock())
  val gt_refclk_n = Input(Clock())
}

trait HasXXVEthernetClocks {
  val rx_core_clk_0 = Input (Clock()) // >= 156.25MHz ... maybe core clock to avoid another crossing?
  val tx_clk_out_0 = Output(Clock()) // AXI4-Stream TX Clock

  val sys_reset = Input(Reset())
  val dclk      = Input(Clock()) // free-running fsm clock
  val rx_clk_out_0  = Output(Clock()) // RX control+status signals

  val user_rx_reset_0 = Output(AsyncReset())
  val user_tx_reset_0 = Output(AsyncReset())
}

trait HasXXVEthernetMAC {
  val stat_rx_block_lock_0 = Output(Bool()) // block lock
  // AXI4-Stream RX
  val rx_axis_tvalid_0 = Output(Bool())
  val rx_axis_tdata_0 = Output(UInt(64.W))
  val rx_axis_tlast_0 = Output(Bool())
  val rx_axis_tkeep_0 = Output(UInt(8.W))
  val rx_axis_tuser_0 = Output(Bool())
  // AXI4-Stream TX
  val tx_axis_tready_0 = Output(Bool())
  val tx_axis_tvalid_0 = Input(Bool())
  val tx_axis_tdata_0 = Input(UInt(64.W))
  val tx_axis_tlast_0 = Input(Bool())
  val tx_axis_tkeep_0 = Input(UInt(8.W))
  val tx_axis_tuser_0 = Input(Bool())
}

trait HasXXVEthernetJunk {
  // Drive these always to 0
  val rx_reset_0 = Input(Reset())
  val tx_reset_0 = Input(Reset())
  val gtwiz_reset_tx_datapath_0 = Input(Reset())
  val gtwiz_reset_rx_datapath_0 = Input(Reset())

  val gtpowergood_out_0 = Output(Bool())
  val gt_refclk_out = Output(Clock()) // 156.25MHz from xcvr refclk pads
  val rxrecclkout_0 = Output(Clock())

  // Drive these always to 3'b101 as per documentation
  val txoutclksel_in_0 = Input(UInt(3.W))
  val rxoutclksel_in_0 = Input(UInt(3.W))

  val stat_rx_framing_err_valid_0 = Output(Bool())
  val stat_rx_framing_err_0       = Output(Bool())
  val stat_rx_hi_ber_0            = Output(Bool())
  val stat_rx_valid_ctrl_code_0   = Output(Bool())
  val stat_rx_bad_code_0          = Output(Bool())
  val stat_rx_local_fault_0       = Output(Bool())
  val stat_rx_status_0            = Output(Bool())
  val stat_tx_local_fault_0       = Output(Bool())
  //New
  val pm_tick_0 = Input(Bool())
  val tx_unfout_0 = Output(Bool())
  val tx_preamblein_0 = Input(UInt(56.W))
  val rx_preambleout_0 = Output(UInt(56.W))
  val ctl_tx_send_rfi_0 = Input(Bool())
  val ctl_tx_send_lfi_0 = Input(Bool())
  val ctl_tx_send_idle_0 = Input(Bool())
  val user_reg0_0 = Output(Bool())
  val qpllreset_in_0 = Input(Bool())
  //Status
  val stat_rx_remote_fault_0 = Output(Bool())
  val stat_rx_bad_fcs_0 = Output(UInt(2.W))
  val stat_rx_stomped_fcs_0 = Output(UInt(2.W))
  val stat_rx_truncated_0 = Output(Bool())
  val stat_rx_internal_local_fault_0 = Output(Bool())
  val stat_rx_received_local_fault_0 = Output(Bool())
  val stat_rx_got_signal_os_0 = Output(Bool())
  val stat_rx_test_pattern_mismatch_0 = Output(Bool())
  val stat_rx_total_bytes_0 = Output(UInt(4.W))
  val stat_rx_total_packets_0 = Output(UInt(2.W))
  val stat_rx_total_good_bytes_0 = Output(UInt(14.W))
  val stat_rx_total_good_packets_0 = Output(Bool())
  val stat_rx_packet_bad_fcs_0 = Output(Bool())
  val stat_rx_packet_64_bytes_0 = Output(Bool())
  val stat_rx_packet_65_127_bytes_0 = Output(Bool())
  val stat_rx_packet_128_255_bytes_0 = Output(Bool())
  val stat_rx_packet_256_511_bytes_0 = Output(Bool())
  val stat_rx_packet_512_1023_bytes_0 = Output(Bool())
  val stat_rx_packet_1024_1518_bytes_0 = Output(Bool())
  val stat_rx_packet_1519_1522_bytes_0 = Output(Bool())
  val stat_rx_packet_1523_1548_bytes_0 = Output(Bool())
  val stat_rx_packet_1549_2047_bytes_0 = Output(Bool())
  val stat_rx_packet_2048_4095_bytes_0 = Output(Bool())
  val stat_rx_packet_4096_8191_bytes_0 = Output(Bool())
  val stat_rx_packet_8192_9215_bytes_0 = Output(Bool())
  val stat_rx_packet_small_0 = Output(Bool())
  val stat_rx_packet_large_0 = Output(Bool())
  val stat_rx_unicast_0 = Output(Bool())
  val stat_rx_multicast_0 = Output(Bool())
  val stat_rx_broadcast_0 = Output(Bool())
  val stat_rx_oversize_0 = Output(Bool())
  val stat_rx_toolong_0 = Output(Bool())
  val stat_rx_undersize_0 = Output(Bool())
  val stat_rx_fragment_0 = Output(Bool())
  val stat_rx_vlan_0 = Output(Bool())
  val stat_rx_inrangeerr_0 = Output(Bool())
  val stat_rx_jabber_0 = Output(Bool())
  val stat_rx_bad_sfd_0 = Output(Bool())
  val stat_rx_bad_preamble_0 = Output(Bool())
  val stat_tx_total_bytes_0 = Output(UInt(2.W))
  val stat_tx_total_packets_0 = Output(Bool())
  val stat_tx_total_good_bytes_0 = Output(UInt(14.W))
  val stat_tx_total_good_packets_0 = Output(Bool())
  val stat_tx_bad_fcs_0 = Output(Bool())
  val stat_tx_packet_64_bytes_0 = Output(Bool())
  val stat_tx_packet_65_127_bytes_0 = Output(Bool())
  val stat_tx_packet_128_255_bytes_0 = Output(Bool())
  val stat_tx_packet_256_511_bytes_0 = Output(Bool())
  val stat_tx_packet_512_1023_bytes_0 = Output(Bool())
  val stat_tx_packet_1024_1518_bytes_0 = Output(Bool())
  val stat_tx_packet_1519_1522_bytes_0 = Output(Bool())
  val stat_tx_packet_1523_1548_bytes_0 = Output(Bool())
  val stat_tx_packet_1549_2047_bytes_0 = Output(Bool())
  val stat_tx_packet_2048_4095_bytes_0 = Output(Bool())
  val stat_tx_packet_4096_8191_bytes_0 = Output(Bool())
  val stat_tx_packet_8192_9215_bytes_0 = Output(Bool())
  val stat_tx_packet_small_0 = Output(Bool())
  val stat_tx_packet_large_0 = Output(Bool())
  val stat_tx_unicast_0 = Output(Bool())
  val stat_tx_multicast_0 = Output(Bool())
  val stat_tx_broadcast_0 = Output(Bool())
  val stat_tx_vlan_0 = Output(Bool())
  val stat_tx_frame_error_0 = Output(Bool())
}

trait HasXXVEthernetAXI4Lite { // AXI4-Lite I/O
  val s_axi_aclk_0 = Input(Clock())
  val s_axi_aresetn_0 = Input(Bool())
  val s_axi_awaddr_0 = Input(UInt(32.W))
  val s_axi_awvalid_0 = Input(Bool())
  val s_axi_awready_0 = Output(Bool())
  val s_axi_wdata_0 = Input(UInt(32.W))
  val s_axi_wstrb_0 = Input(UInt(4.W))
  val s_axi_wvalid_0 = Input(Bool())
  val s_axi_wready_0 = Output(Bool())
  val s_axi_bresp_0 = Output(UInt(2.W))
  val s_axi_bvalid_0 = Output(Bool())
  val s_axi_bready_0 = Input(Bool())
  val s_axi_araddr_0 = Input(UInt(32.W))
  val s_axi_arvalid_0 = Input(Bool())
  val s_axi_arready_0 = Output(Bool())
  val s_axi_rdata_0 = Output(UInt(32.W))
  val s_axi_rresp_0 = Output(UInt(2.W))
  val s_axi_rvalid_0 = Output(Bool())
  val s_axi_rready_0 = Input(Bool())
}

class XXVEthernetBlackBoxIO extends Bundle
  with HasXXVEthernetPads
  with HasXXVEthernetClocks
  with HasXXVEthernetMAC
  with HasXXVEthernetJunk
  with HasXXVEthernetAXI4Lite

class XXVEthernetPads() extends Bundle with HasXXVEthernetPads
class XXVEthernetMAC() extends Bundle with HasXXVEthernetMAC
class XXVEthernetClocks() extends Bundle with HasXXVEthernetClocks
class XXVEthernetAXI4Lite() extends Bundle with HasXXVEthernetAXI4Lite

trait XXVEthernetParamsBase

case class XXVEthernetParams(
  name:    String,
  speed:   Int,
  dclkMHz: Double) extends XXVEthernetParamsBase with DeviceParams
{
  require (speed == 10 || speed == 25)
  val refMHz = if (speed == 10) 156.25 else 161.1328125
}

class XXVEthernetBlackBox(c: XXVEthernetParams) extends BlackBox
{
  override def desiredName = c.name

  val io = IO(new XXVEthernetBlackBoxIO)

  ElaborationArtefacts.add(s"${desiredName}.vivado.tcl",
    s"""create_ip -vendor xilinx.com -library ip -version 4.0 -name xxv_ethernet -module_name ${desiredName} -dir $$ipdir -force
       |set_property -dict [list 							\\
       |  CONFIG.CORE				{Ethernet MAC+PCS/PMA 64-bit}		\\
       |  CONFIG.GT_DRP_CLK			{${c.dclkMHz}}				\\
       |  CONFIG.GT_REF_CLK_FREQ		{${c.refMHz}}				\\
       |  CONFIG.INCLUDE_AXI4_INTERFACE 	{1}					\\
       |  CONFIG.INCLUDE_STATISTICS_COUNTERS 	{0}					\\
       |  CONFIG.BASE_R_KR			{BASE-R}				\\
       |  CONFIG.LINE_RATE			{${c.speed}}				\\
       |  CONFIG.NUM_OF_CORES			{1}					\\
       |] [get_ips ${desiredName}]
       |""".stripMargin)
}

class AXI4StreamFIFO() extends BlackBox { // Xilinx AXI4-Stream FIFO IP for clock domain crossing between 100MHz and 156.25MHz
  override def desiredName = "axis_data_fifo_0"

  val io = IO(new Bundle {
    val s_axis_aresetn = Input(Reset())
    val s_axis_aclk = Input(Clock())
    val s_axis_tvalid = Input(Bool())
    val s_axis_tready = Output(Bool())
    val s_axis_tdata = Input(UInt(64.W))
    val s_axis_tkeep = Input(UInt(8.W))
    val s_axis_tlast = Input(Bool())
    val m_axis_aclk = Input(Clock())
    val m_axis_tvalid = Output(Bool())
    val m_axis_tready = Input(Bool())
    val m_axis_tdata = Output(UInt(64.W))
    val m_axis_tkeep = Output(UInt(8.W))
    val m_axis_tlast = Output(Bool())
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

class DiplomaticXXVEthernet(c: XXVEthernetParams)(implicit p:Parameters) extends LazyModule with CrossesToOnlyOneClockDomain
{
  val crossing = SynchronousCrossing()
  val node = AXI4SlaveNode(Seq(AXI4SlavePortParameters( // Node for connecting AXI4-Lite interfaces
  Seq(AXI4SlaveParameters(
    address       = List(AddressSet(0x60000000,0xfff)),
    regionType    = RegionType.UNCACHED,
    executable    = true,
    supportsRead  = TransferSizes(1, 4),
    supportsWrite = TransferSizes(1, 4),
    interleavedId = Some(0))),
  beatBytes  = 4,
  // requestKeys = Seq(AMBACorrupt),
  minLatency = 1)))

  lazy val module = new LazyRawModuleImp(this) {
    val (in, edgeIn) = node.in(0)
    val io = IO(new Bundle {
      val pads   = new XXVEthernetPads
      val mac    = new XXVEthernetMAC
      val clocks = new XXVEthernetClocks
      val axi4lite = new XXVEthernetAXI4Lite
    })

    val blackbox = Module(new XXVEthernetBlackBox(c))


    //AXI-Lite
    in.aw.ready := blackbox.io.s_axi_awready_0
    in.w.ready := blackbox.io.s_axi_wready_0
    in.b.bits.resp := blackbox.io.s_axi_bresp_0
    in.b.valid := blackbox.io.s_axi_bvalid_0
    in.ar.ready := blackbox.io.s_axi_arready_0
    in.r.bits.data := blackbox.io.s_axi_rdata_0
    in.r.bits.resp := blackbox.io.s_axi_rresp_0
    in.r.valid := blackbox.io.s_axi_rvalid_0
    blackbox.io.s_axi_aclk_0 := io.axi4lite.s_axi_aclk_0
    blackbox.io.s_axi_aresetn_0 := io.axi4lite.s_axi_aresetn_0
    blackbox.io.s_axi_awaddr_0 := in.aw.bits.addr
    blackbox.io.s_axi_awvalid_0 := in.aw.valid
    blackbox.io.s_axi_wdata_0 := in.w.bits.data
    blackbox.io.s_axi_wstrb_0 := in.w.bits.strb
    blackbox.io.s_axi_wvalid_0 := in.w.valid
    blackbox.io.s_axi_bready_0 := in.b.ready
    blackbox.io.s_axi_araddr_0 := in.ar.bits.addr
    blackbox.io.s_axi_arvalid_0 := in.ar.valid
    blackbox.io.s_axi_rready_0 := in.r.ready

    // pads
    io.pads.gt_txp_out_0 := blackbox.io.gt_txp_out_0
    io.pads.gt_txn_out_0 := blackbox.io.gt_txn_out_0
    blackbox.io.gt_rxp_in_0 := io.pads.gt_rxp_in_0
    blackbox.io.gt_rxn_in_0 := io.pads.gt_rxn_in_0
    blackbox.io.gt_refclk_p := io.pads.gt_refclk_p
    blackbox.io.gt_refclk_n := io.pads.gt_refclk_n

    // clocks
    io.clocks.tx_clk_out_0    := blackbox.io.tx_clk_out_0
    io.clocks.user_rx_reset_0 := blackbox.io.user_rx_reset_0
    io.clocks.user_tx_reset_0 := blackbox.io.user_tx_reset_0
    blackbox.io.rx_core_clk_0 := io.clocks.rx_core_clk_0
    blackbox.io.sys_reset     := io.clocks.sys_reset
    blackbox.io.dclk          := io.clocks.dclk
    io.clocks.rx_clk_out_0    := blackbox.io.rx_clk_out_0
    // MAC
    io.mac.stat_rx_block_lock_0 := blackbox.io.stat_rx_block_lock_0
    // MAC--AXI4Stream
    blackbox.io.tx_axis_tvalid_0 := io.mac.tx_axis_tvalid_0
    blackbox.io.tx_axis_tdata_0 := io.mac.tx_axis_tdata_0
    blackbox.io.tx_axis_tlast_0 := io.mac.tx_axis_tlast_0
    blackbox.io.tx_axis_tkeep_0 := io.mac.tx_axis_tkeep_0
    blackbox.io.tx_axis_tuser_0 := io.mac.tx_axis_tuser_0
    io.mac.tx_axis_tready_0 := blackbox.io.tx_axis_tready_0

    io.mac.rx_axis_tvalid_0 := blackbox.io.rx_axis_tvalid_0
    io.mac.rx_axis_tdata_0 := blackbox.io.rx_axis_tdata_0
    io.mac.rx_axis_tlast_0 := blackbox.io.rx_axis_tlast_0
    io.mac.rx_axis_tkeep_0 := blackbox.io.rx_axis_tkeep_0
    io.mac.rx_axis_tuser_0 := blackbox.io.rx_axis_tuser_0

    // Junk
    blackbox.io.txoutclksel_in_0 := 5.U
    blackbox.io.rxoutclksel_in_0 := 5.U
    blackbox.io.rx_reset_0                := io.clocks.sys_reset
    blackbox.io.tx_reset_0                := io.clocks.sys_reset
    blackbox.io.gtwiz_reset_tx_datapath_0 := io.clocks.sys_reset
    blackbox.io.gtwiz_reset_rx_datapath_0 := io.clocks.sys_reset
	  //New
    blackbox.io.pm_tick_0 := false.B
    blackbox.io.tx_preamblein_0 := 0.U
    blackbox.io.ctl_tx_send_rfi_0 := false.B
    blackbox.io.ctl_tx_send_lfi_0 := false.B
    blackbox.io.ctl_tx_send_idle_0 := false.B
    blackbox.io.qpllreset_in_0 := false.B
  }
}

class XXVEthernet(config: XXVEthernetParams)(implicit p: Parameters) extends LazyModule {
  // Component for connecting AXI4-Lite nodes
  val xbar = LazyModule(new AXI4Xbar)
  val frag = LazyModule(new AXI4Fragmenter)
  val buf = LazyModule(new AXI4Buffer)
  // island module containing the Ethernet Subsystem IP
  val island = LazyModule(new DiplomaticXXVEthernet(config))
  // OX core
  val ox = LazyModule(new OmniXtendNode()(p))
  // Connecting AXI4-Lite nodes
  val node: AXI4InwardNode =
    island.node := buf.node := frag.node := xbar.node

  // Connecting TileLink nodes
  val node2: TLInwardNode =
    ox.node := TLBuffer() := TLWidthWidget(8) := TLXbar()

  lazy val module = new LazyModuleImp(this) {
    val ox2ethfifo = Module(new AXI4StreamFIFO())
    val eth2oxfifo = Module(new AXI4StreamFIFO())
    val vio = Module(new VIO())
    val io = IO(new Bundle {
      val pads   = new XXVEthernetPads
      val mac    = new XXVEthernetMAC
      val clocks = new XXVEthernetClocks
      val axi4lite = new XXVEthernetAXI4Lite
      // Adding GPIO input
      val gpio_sw_s = Input(Bool())
      val gpio_sw_n = Input(Bool())
      val gpio_sw_w = Input(Bool())
      val gpio_sw_e = Input(Bool())
    })

    io.pads <> island.module.io.pads
    io.mac <> island.module.io.mac
    io.clocks <> island.module.io.clocks
    io.axi4lite.s_axi_aclk_0 <> island.module.io.axi4lite.s_axi_aclk_0
    io.axi4lite.s_axi_aresetn_0 <> island.module.io.axi4lite.s_axi_aresetn_0    

    //OX to AXI-Stream (TX)
    island.module.io.mac.tx_axis_tuser_0 := 0.U

    ox2ethfifo.io.s_axis_tvalid := ox.module.io.txvalid
    ox2ethfifo.io.s_axis_tdata := ox.module.io.txdata
    ox2ethfifo.io.s_axis_tlast := ox.module.io.txlast
    ox2ethfifo.io.s_axis_tkeep := ox.module.io.txkeep
    ox.module.io.txready := ox2ethfifo.io.s_axis_tready

    island.module.io.mac.tx_axis_tvalid_0 := ox2ethfifo.io.m_axis_tvalid
    island.module.io.mac.tx_axis_tdata_0 := ox2ethfifo.io.m_axis_tdata
    island.module.io.mac.tx_axis_tlast_0 := ox2ethfifo.io.m_axis_tlast
    island.module.io.mac.tx_axis_tkeep_0 := ox2ethfifo.io.m_axis_tkeep

    ox2ethfifo.io.s_axis_aresetn := !Module.reset.asBool
    ox2ethfifo.io.s_axis_aclk := Module.clock
    ox2ethfifo.io.m_axis_aclk := island.module.io.clocks.tx_clk_out_0

    //AXI-Stream to OX (RX)
    eth2oxfifo.io.s_axis_tvalid := island.module.io.mac.rx_axis_tvalid_0
    eth2oxfifo.io.s_axis_tdata := island.module.io.mac.rx_axis_tdata_0
    eth2oxfifo.io.s_axis_tlast := island.module.io.mac.rx_axis_tlast_0
    eth2oxfifo.io.s_axis_tkeep := island.module.io.mac.rx_axis_tkeep_0

    eth2oxfifo.io.s_axis_aresetn := !Module.reset.asBool
    eth2oxfifo.io.s_axis_aclk := island.module.io.clocks.rx_clk_out_0
    eth2oxfifo.io.m_axis_aclk := Module.clock

    eth2oxfifo.io.m_axis_tready := true.B

    ox.module.io.rxdata := eth2oxfifo.io.m_axis_tdata
    ox.module.io.rxvalid := eth2oxfifo.io.m_axis_tvalid
    ox.module.io.rxlast := eth2oxfifo.io.m_axis_tlast

    //vio
    vio.io.clk := Module.clock
    val vio_reg0 = RegInit(false.B)
    val vio_reg1 = RegInit(false.B)
    val vio_reg2 = RegInit(false.B)
    val vio_reg3 = RegInit(false.B)
    val ox_open_vio = RegInit(false.B)
    val ox_open_btn = RegInit(false.B)
    val ox_close_vio = RegInit(false.B)
    val ox_close_btn = RegInit(false.B)
    val debug1_vio = RegInit(false.B)
    val debug1_btn = RegInit(false.B)
    val debug2_vio = RegInit(false.B)
    val debug2_btn = RegInit(false.B)
    val vio_test_reg = RegInit(0.U(32.W))
    val debug1 = RegInit(false.B)
    val debug2 = RegInit(false.B)
    vio_reg0 := vio.io.probe_out0
    vio_reg1 := vio.io.probe_out1
    vio_reg2 := vio.io.probe_out2
    vio_reg3 := vio.io.probe_out3
    ox_open_vio := vio.io.probe_out0 && !vio_reg0
    ox_close_vio := vio.io.probe_out1 && !vio_reg1
    debug1_vio := vio.io.probe_out2 && !vio_reg2
    debug2_vio := vio.io.probe_out3 && !vio_reg3

    //gpio
    //ox_open
    val idle0 :: wait0 :: Nil = Enum(2)
    val state0 = RegInit(idle0)
    val filt_cnt0 = RegInit(0.U(32.W))
    val gpio_reg0 = RegInit(false.B)
    gpio_reg0 := io.gpio_sw_s
    when(state0 === idle0){
      when(io.gpio_sw_s){
        ox_open_btn := io.gpio_sw_s && !gpio_reg0
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
    val idle1 :: wait1 :: Nil = Enum(2)
    val state1 = RegInit(idle1)
    val filt_cnt1 = RegInit(0.U(32.W))
    val gpio_reg1 = RegInit(false.B)
    gpio_reg1 := io.gpio_sw_n
    when(state1 === idle1){
      when(io.gpio_sw_n){
        ox_close_btn := io.gpio_sw_n && !gpio_reg1
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
    val idle2 :: wait2 :: Nil = Enum(2)
    val state2 = RegInit(idle2)
    val filt_cnt2 = RegInit(0.U(32.W))
    val gpio_reg2 = RegInit(false.B)
    gpio_reg2 := io.gpio_sw_w
    when(state2 === idle2){
      when(io.gpio_sw_w){
        debug1_btn := io.gpio_sw_w && !gpio_reg2
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
    val idle3 :: wait3 :: Nil = Enum(2)
    val state3 = RegInit(idle3)
    val filt_cnt3 = RegInit(0.U(32.W))
    val gpio_reg3 = RegInit(false.B)
    gpio_reg3 := io.gpio_sw_e
    when(state3 === idle3){
      when(io.gpio_sw_e){
        debug2_btn := io.gpio_sw_e && !gpio_reg3
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
    ox.module.io.ox_open := ox_open_vio || ox_open_btn
    ox.module.io.ox_close := ox_close_vio || ox_close_btn
    ox.module.io.debug1 := debug1_vio || debug1_btn
    ox.module.io.debug2 := debug2_vio || debug2_btn
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
