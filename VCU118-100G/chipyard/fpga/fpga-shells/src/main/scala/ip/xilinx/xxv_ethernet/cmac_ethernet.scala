package sifive.fpgashells.ip.xilinx.xxv_ethernet

import chisel3._
import chisel3.util._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util.{ElaborationArtefacts}
import freechips.rocketchip.subsystem._
import freechips.rocketchip.tilelink._
import omnixtend._

trait HasCMACEthernetPads {
  val gt_txn_out = Output(UInt(4.W))
  val gt_txp_out = Output(UInt(4.W))
  val gt_rxp_in = Input(UInt(4.W))
  val gt_rxn_in = Input(UInt(4.W))
  // val gt0_txn_out = Output(Bool())
  // val gt0_txp_out = Output(Bool())
  // val gt1_txn_out = Output(Bool())
  // val gt1_txp_out = Output(Bool())
  // val gt2_txn_out = Output(Bool())
  // val gt2_txp_out = Output(Bool())
  // val gt3_txn_out = Output(Bool())
  // val gt3_txp_out = Output(Bool())
  // val gt0_rxp_in = Input(Bool())
  // val gt0_rxn_in = Input(Bool())
  // val gt1_rxp_in = Input(Bool())
  // val gt1_rxn_in = Input(Bool())
  // val gt2_rxp_in = Input(Bool())
  // val gt2_rxn_in = Input(Bool())
  // val gt3_rxp_in = Input(Bool())
  // val gt3_rxn_in = Input(Bool())
  val gt_ref_clk_p = Input(Clock())
  val gt_ref_clk_n = Input(Clock())
}

trait HasCMACEthernetClocks {
  val rx_clk = Input (Clock())
  val gt_txusrclk2 = Output(Clock())

  val sys_reset = Input(Reset())
  val drp_clk      = Input(Clock())
  val gt_rxusrclk2 = Output(Clock())

  val usr_rx_reset = Output(AsyncReset())
  val usr_tx_reset = Output(AsyncReset())

  val init_clk = Input(Clock())
}

trait HasCMACEthernetMAC {
  val stat_rx_block_lock = Output(UInt(20.W))
  // RX
  val rx_axis_tvalid = Output(Bool())
  val rx_axis_tdata = Output(UInt(512.W))
  val rx_axis_tlast = Output(Bool())
  val rx_axis_tkeep = Output(UInt(64.W))
  val rx_axis_tuser = Output(Bool())
  // TX
  val tx_axis_tready = Output(Bool())
  val tx_axis_tvalid = Input(Bool())
  val tx_axis_tdata = Input(UInt(512.W))
  val tx_axis_tlast = Input(Bool())
  val tx_axis_tkeep = Input(UInt(64.W))
  val tx_axis_tuser = Input(Bool())
}

trait HasCMACEthernetJunk {
  val core_rx_reset = Input(Bool())
  val core_tx_reset = Input(Bool())
  val gtwiz_reset_tx_datapath = Input(Bool())
  val gtwiz_reset_rx_datapath = Input(Bool())
  val tx_preamblein = Input(UInt(56.W))
  val ctl_tx_send_idle = Input(Bool())
  val ctl_tx_send_rfi = Input(Bool())
  val ctl_tx_send_lfi = Input(Bool())
  val gt_loopback_in = Input(UInt(12.W))
  val ctl_rx_enable = Input(Bool())
  val ctl_rx_force_resync = Input(Bool())
  val ctl_rx_test_pattern = Input(Bool())
  val ctl_tx_enable = Input(Bool())
  val ctl_tx_test_pattern = Input(Bool())
  val core_drp_reset = Input(Bool())
  val drp_addr = Input(UInt(10.W))
  val drp_di = Input(UInt(16.W))
  val drp_en = Input(Bool())
  val drp_we = Input(Bool())
}

class CMACEthernetBlackBoxIO extends Bundle
  with HasCMACEthernetPads
  with HasCMACEthernetClocks
  with HasCMACEthernetMAC
  with HasCMACEthernetJunk

class CMACEthernetPads() extends Bundle with HasCMACEthernetPads
class CMACEthernetMAC() extends Bundle with HasCMACEthernetMAC
class CMACEthernetClocks() extends Bundle with HasCMACEthernetClocks

class CMACEthernetBlackBox() extends BlackBox
{
  override def desiredName = "cmac_usplus_0"

  val io = IO(new CMACEthernetBlackBoxIO)

  ElaborationArtefacts.add(s"${desiredName}.vivado.tcl",
    s"""create_ip -vendor xilinx.com -library ip -version 3.1 -name cmac_usplus -module_name cmac_usplus_0 -dir $$ipdir -force
       |set_property -dict [list 							\\
       |  CONFIG.CMAC_CAUI4_MODE		{1}					\\
       |  CONFIG.NUM_LANES			{4x25}					\\
       |  CONFIG.GT_REF_CLK_FREQ		{156.25}				\\
       |  CONFIG.USER_INTERFACE			{AXIS}					\\
       |  CONFIG.GT_DRP_CLK			{125}					\\
       |  CONFIG.TX_FLOW_CONTROL		{0}					\\
       |  CONFIG.RX_FLOW_CONTROL		{0}					\\
       |  CONFIG.ENABLE_AXI_INTERFACE		{0}					\\
       |  CONFIG.INCLUDE_STATISTICS_COUNTERS	{0}					\\
       |  CONFIG.GT_LOCATION			{1}					\\
       |  CONFIG.CMAC_CORE_SELECT		{CMACE4_X0Y7}				\\
       |  CONFIG.GT_GROUP_SELECT		{X1Y48~X1Y51}				\\
       |  CONFIG.LANE1_GT_LOC			{X1Y48}					\\
       |  CONFIG.LANE2_GT_LOC			{X1Y49}					\\
       |  CONFIG.LANE3_GT_LOC			{X1Y50}					\\
       |  CONFIG.LANE4_GT_LOC			{X1Y51}					\\
       |  CONFIG.LANE5_GT_LOC			{NA}					\\
       |  CONFIG.LANE6_GT_LOC			{NA}					\\
       |  CONFIG.LANE7_GT_LOC			{NA}					\\
       |  CONFIG.LANE8_GT_LOC			{NA}					\\
       |  CONFIG.LANE9_GT_LOC			{NA}					\\
       |  CONFIG.LANE10_GT_LOC			{NA}					\\
       |  CONFIG.INCLUDE_SHARED_LOGIC		{2}					\\
       |  CONFIG.RX_GT_BUFFER			{NA}					\\
       |  CONFIG.GT_RX_BUFFER_BYPASS		{NA}					\\
       |  CONFIG.ADD_GT_CNRL_STS_PORTS		{0}					\\
       |] [get_ips cmac_usplus_0]
       |""".stripMargin)
}

class AXI4StreamFIFO_10G() extends BlackBox
{
  override def desiredName = "axis_data_fifo_0"

  val io = IO(new Bundle {
    val s_axis_aresetn = Input(Reset())
    val s_axis_aclk = Input(Clock())
    val s_axis_tvalid = Input(Bool())
    val s_axis_tready = Output(Bool())
    val s_axis_tdata = Input(UInt(512.W))
    val s_axis_tkeep = Input(UInt(64.W))
    val s_axis_tlast = Input(Bool())
    val m_axis_aclk = Input(Clock())
    val m_axis_tvalid = Output(Bool())
    val m_axis_tready = Input(Bool())
    val m_axis_tdata = Output(UInt(512.W))
    val m_axis_tkeep = Output(UInt(64.W))
    val m_axis_tlast = Output(Bool())
  })
  ElaborationArtefacts.add(s"axis_data_fifo_0.vivado.tcl",
    s"""create_ip -name axis_data_fifo -vendor xilinx.com -library ip -version 2.0 -module_name axis_data_fifo_0 -dir $$ipdir -force
       |set_property -dict [list 							\\
       |  CONFIG.TDATA_NUM_BYTES {64}		\\
       |  CONFIG.FIFO_MODE {2}				\\
       |  CONFIG.IS_ACLK_ASYNC {1}				\\
       |  CONFIG.HAS_TKEEP {1}					\\
       |  CONFIG.HAS_TLAST {1}					\\
       |] [get_ips axis_data_fifo_0]
       |""".stripMargin)
}

class PacketGen() extends BlackBox with HasBlackBoxResource {
  override def desiredName = "PacketGen"
  val io = IO(new Bundle{
    val clk = Input(Clock())
    val rst = Input(Reset())
    val test_mode = Input(Bool())
    val m_axis_tready = Input(Bool())
    val m_axis_tvalid = Output(Bool())
    val m_axis_tdata = Output(UInt(512.W))
    val m_axis_tlast = Output(Bool())
    val m_axis_tkeep = Output(UInt(64.W))
    val o_thr_cnt = Output(UInt(14.W))
    val o_thr_valid = Output(Bool())
  })
  addResource("/vsrc/packetgen.v")
}

class LatencyGen() extends BlackBox with HasBlackBoxResource {
  override def desiredName = "LatencyGen"
  val io = IO(new Bundle{
    val clk = Input(Clock())
    val rst = Input(Reset())
    val test_mode = Input(Bool())
    val m_axis_tready = Input(Bool())
    val s_axis_tlast = Input(Bool())
    val m_axis_tlast = Input(Bool())
    val o_lat_cnt = Output(UInt(14.W))
    val o_lat_valid = Output(Bool())
  })
  addResource("/vsrc/latencygen.v")
}

class VIO_100G() extends BlackBox {
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

class DiplomaticCMACEthernet()(implicit p: Parameters) extends LazyModule
{
  lazy val module = new LazyRawModuleImp(this) {
    val io = IO(new Bundle {
    val pads   = new CMACEthernetPads
    val mac    = new CMACEthernetMAC
    val clocks = new CMACEthernetClocks
  })

  val blackbox = Module(new CMACEthernetBlackBox())

  // pads
  io.pads.gt_txp_out := blackbox.io.gt_txp_out
  io.pads.gt_txn_out := blackbox.io.gt_txn_out
  blackbox.io.gt_rxp_in := io.pads.gt_rxp_in
  blackbox.io.gt_rxn_in := io.pads.gt_rxn_in

  // io.pads.gt0_txp_out := blackbox.io.gt0_txp_out
  // io.pads.gt0_txn_out := blackbox.io.gt0_txn_out
  // io.pads.gt1_txp_out := blackbox.io.gt1_txp_out
  // io.pads.gt1_txn_out := blackbox.io.gt1_txn_out
  // io.pads.gt2_txp_out := blackbox.io.gt2_txp_out
  // io.pads.gt2_txn_out := blackbox.io.gt2_txn_out
  // io.pads.gt3_txp_out := blackbox.io.gt3_txp_out
  // io.pads.gt3_txn_out := blackbox.io.gt3_txn_out
  // blackbox.io.gt0_rxp_in := io.pads.gt0_rxp_in
  // blackbox.io.gt0_rxn_in := io.pads.gt0_rxn_in
  // blackbox.io.gt1_rxp_in := io.pads.gt1_rxp_in
  // blackbox.io.gt1_rxn_in := io.pads.gt1_rxn_in
  // blackbox.io.gt2_rxp_in := io.pads.gt2_rxp_in
  // blackbox.io.gt2_rxn_in := io.pads.gt2_rxn_in
  // blackbox.io.gt3_rxp_in := io.pads.gt3_rxp_in
  // blackbox.io.gt3_rxn_in := io.pads.gt3_rxn_in
  blackbox.io.gt_ref_clk_p := io.pads.gt_ref_clk_p
  blackbox.io.gt_ref_clk_n := io.pads.gt_ref_clk_n

  // clocks
  io.clocks.gt_txusrclk2 := blackbox.io.gt_txusrclk2
  io.clocks.usr_rx_reset := blackbox.io.usr_rx_reset
  io.clocks.usr_tx_reset := blackbox.io.usr_tx_reset
  blackbox.io.rx_clk := io.clocks.rx_clk
  blackbox.io.sys_reset := io.clocks.sys_reset
  blackbox.io.drp_clk := io.clocks.drp_clk
  io.clocks.gt_rxusrclk2 := blackbox.io.gt_rxusrclk2
  blackbox.io.init_clk := io.clocks.init_clk

  // MAC
  io.mac.stat_rx_block_lock := blackbox.io.stat_rx_block_lock
  // AXI4Stream TX
  blackbox.io.tx_axis_tvalid := io.mac.tx_axis_tvalid
  blackbox.io.tx_axis_tdata := io.mac.tx_axis_tdata
  blackbox.io.tx_axis_tlast := io.mac.tx_axis_tlast
  blackbox.io.tx_axis_tkeep := io.mac.tx_axis_tkeep
  blackbox.io.tx_axis_tuser := io.mac.tx_axis_tuser
  io.mac.tx_axis_tready := blackbox.io.tx_axis_tready
  // AXI4Stream RX
  io.mac.rx_axis_tvalid := blackbox.io.rx_axis_tvalid
  io.mac.rx_axis_tdata := blackbox.io.rx_axis_tdata
  io.mac.rx_axis_tlast := blackbox.io.rx_axis_tlast
  io.mac.rx_axis_tkeep := blackbox.io.rx_axis_tkeep
  io.mac.rx_axis_tuser := blackbox.io.rx_axis_tuser

  //Junk
  blackbox.io.core_rx_reset := io.clocks.sys_reset
  blackbox.io.core_tx_reset := io.clocks.sys_reset
  blackbox.io.gtwiz_reset_tx_datapath := io.clocks.sys_reset
  blackbox.io.gtwiz_reset_rx_datapath := io.clocks.sys_reset
  blackbox.io.tx_preamblein := 0.U
  blackbox.io.ctl_tx_send_rfi := false.B
  blackbox.io.ctl_tx_send_lfi := false.B
  blackbox.io.ctl_tx_send_idle := false.B
  blackbox.io.gt_loopback_in := 0.U
  blackbox.io.ctl_rx_enable := true.B
  blackbox.io.ctl_rx_force_resync := false.B
  blackbox.io.ctl_rx_test_pattern := false.B
  blackbox.io.ctl_tx_enable := true.B
  blackbox.io.ctl_tx_test_pattern := false.B
  blackbox.io.core_drp_reset := false.B
  blackbox.io.drp_addr := 0.U
  blackbox.io.drp_di := 0.U
  blackbox.io.drp_en := false.B
  blackbox.io.drp_we := false.B
  }
}

class MemBundle() extends Bundle {
  val regData_latency = Input(UInt(64.W))
  val regData_throughput = Input(UInt(64.W))
  val regData_latency_valid = Input(Bool())
  val regData_throuhgput_valid = Input(Bool())
  val readData = Output(UInt(64.W))
}

class MemNode(implicit p: Parameters) extends LazyModule {
  val beatBytes = 8
  val node = TLManagerNode(Seq(TLSlavePortParameters.v1(Seq(TLSlaveParameters.v1(
    address            = Seq(AddressSet(0x100000000L, 0x0FFFFFFFL)), // Address range this node responds to
    resources          = new SimpleDevice("omnixtend", Seq("example,omnixtend")).reg, // Device resources
    regionType         = RegionType.UNCACHED, // Memory region type
    executable         = true, // Memory is executable
    supportsGet        = TransferSizes(1, beatBytes), // Supported transfer sizes for Get operations
    supportsPutFull    = TransferSizes(1, beatBytes), // Supported transfer sizes for PutFull operations
    supportsPutPartial = TransferSizes(1, beatBytes), // Supported transfer sizes for PutPartial operations
    fifoId             = Some(0) // FIFO ID
  )),
    beatBytes          = 8, // Beat size for the port
    minLatency         = 1 // Minimum latency for the port
  )))

  lazy val module = new LazyModuleImp(this) {
    val io = IO(new MemBundle)

    val (in, edge) = node.in(0)

    val aValidReg   = RegInit(false.B) // Register to store the validity of any operation
    val opcodeReg   = RegInit(0.U(3.W)) // Register to store the opcode
    val sourceReg   = RegInit(0.U(4.W)) // Register to store the source ID
    val sizeReg     = RegInit(0.U(3.W)) // Register to store the size
    val paramReg    = RegInit(0.U(2.W)) // Register to store the parameter
    val regData     = RegInit(0.U(64.W))
    val readData    = RegInit(0.U(64.W))
    val condition   = RegInit(false.B)

    val regmem = SyncReadMem(131072, UInt(64.W))

    when(in.a.valid) {
      opcodeReg := Mux(in.a.bits.opcode === TLMessages.Get, TLMessages.AccessAckData, TLMessages.AccessAck)
      sourceReg := in.a.bits.source
      sizeReg   := in.a.bits.size
      paramReg  := in.a.bits.param
      readData := regmem.read(in.a.bits.address)
      condition := in.a.valid
    }.elsewhen(io.regData_latency_valid) {
      regmem.write(0x0.U, io.regData_latency)
    }.elsewhen(io.regData_throuhgput_valid) {
      regmem.write(0x4.U, io.regData_throughput)
    }

    // Default values for the response channel 'd'
    in.d.valid        := false.B
    in.d.bits.opcode  := 0.U
    in.d.bits.param   := 0.U
    in.d.bits.size    := 0.U
    in.d.bits.source  := 0.U
    in.d.bits.sink    := 0.U
    in.d.bits.denied  := false.B
    in.d.bits.data    := 0.U
    in.d.bits.corrupt := false.B

    when (condition) {
        in.d.valid        := true.B // Mark the response as valid
        in.d.bits         := edge.AccessAck(in.a.bits) // Generate an AccessAck response
        in.d.bits.opcode  := opcodeReg // Set the opcode from the register
        in.d.bits.param   := paramReg // Set the parameter from the register
        in.d.bits.size    := sizeReg // Set the size from the register
        in.d.bits.source  := sourceReg // Set the source ID from the register
        in.d.bits.sink    := 0.U // Set sink to 0
        in.d.bits.denied  := false.B // Mark as not denied
        in.d.bits.data    := readData // Set the data from the received data
        in.d.bits.corrupt := false.B // Mark as not corrupt
    }

    // Ready conditions for the input channel 'a' and response channel 'd'
    in.a.ready := true.B
    in.d.ready := true.B

    io.readData := readData
  }
}

class CMACEthernet()(implicit p: Parameters) extends LazyModule {
  //100G Ethernet IP
  val island = LazyModule(new DiplomaticCMACEthernet())
  //OX
  val ox = LazyModule(new OmniXtendNode()(p))
  val mem = LazyModule(new MemNode()(p))
  //node
  val node2: TLInwardNode =
    ox.node := TLBuffer() := TLWidthWidget(8) := TLXbar()

  val node3: TLInwardNode =
    mem.node := TLBuffer() := TLWidthWidget(8) := TLXbar()
  
  lazy val module = new LazyModuleImp(this) {
    val ox2ethfifo = Module(new AXI4StreamFIFO_10G())
    val eth2oxfifo = Module(new AXI4StreamFIFO_10G())
    val pkt_gen = Module(new PacketGen())
    val lat_gen = Module(new LatencyGen())
    val vio = Module(new VIO_100G())
    val io = IO(new Bundle {
      val pads   = new CMACEthernetPads
      val mac    = new CMACEthernetMAC
      val clocks = new CMACEthernetClocks
      val gpio_sw_s = Input(Bool())
      val gpio_sw_n = Input(Bool())
      val gpio_sw_w = Input(Bool())
      val gpio_sw_e = Input(Bool())
    })

    io.pads <> island.module.io.pads
    io.mac <> island.module.io.mac
    io.clocks <> island.module.io.clocks

    ox.module.clock := island.module.io.clocks.gt_txusrclk2

    //OX - Ethernet
    val keep32 = "hFFFFFFFF".U(32.W)
    island.module.io.mac.tx_axis_tuser := 0.U
    island.module.io.mac.tx_axis_tvalid := ox.module.io.txvalid
    island.module.io.mac.tx_axis_tdata := ox.module.io.txdata
    island.module.io.mac.tx_axis_tlast := ox.module.io.txlast
    island.module.io.mac.tx_axis_tkeep := (keep32 << 32) | keep32

    ox.module.io.txready := island.module.io.mac.tx_axis_tready
    ox.module.io.rxdata := island.module.io.mac.rx_axis_tdata
    ox.module.io.rxvalid := island.module.io.mac.rx_axis_tvalid
    ox.module.io.rxlast := island.module.io.mac.rx_axis_tlast

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
    ox.module.io.ox_open := ox_open_vio || ox_open_btn
    ox.module.io.ox_close := ox_close_vio || ox_close_btn
    ox.module.io.debug1 := debug1_vio || debug1_btn
    debug1 := debug1_vio || debug1_btn
    ox.module.io.debug2 := debug2_vio || debug2_btn
    debug2 := debug2_vio || debug2_btn
    //throughput gen
    pkt_gen.io.clk := island.module.io.clocks.gt_txusrclk2
    pkt_gen.io.rst := !Module.reset.asBool
    val test_mode_throughput = RegInit(false.B)
    pkt_gen.io.test_mode := test_mode_throughput
    //latency gen
    lat_gen.io.clk := island.module.io.clocks.gt_txusrclk2
    lat_gen.io.rst := !Module.reset.asBool
    val test_mode_latency = RegInit(false.B)
    lat_gen.io.test_mode := test_mode_latency
    when(debug1){
      test_mode_throughput := !test_mode_throughput
    }
    when(debug2){
      test_mode_latency := !test_mode_latency
    }
    mem.module.io.regData_latency := lat_gen.io.o_lat_cnt
    mem.module.io.regData_throughput := pkt_gen.io.o_thr_cnt
    mem.module.io.regData_latency_valid := lat_gen.io.o_lat_valid
    mem.module.io.regData_throuhgput_valid := pkt_gen.io.o_thr_valid
  }
}
