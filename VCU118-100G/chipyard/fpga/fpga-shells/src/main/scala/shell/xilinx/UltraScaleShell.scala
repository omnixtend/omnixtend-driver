package sifive.fpgashells.shell.xilinx

import chisel3._
import chisel3.util._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import sifive.fpgashells.clocks._
import sifive.fpgashells.shell._
import sifive.fpgashells.ip.xilinx._
import sifive.fpgashells.devices.xilinx.xdma._
import sifive.fpgashells.devices.xilinx.ethernet._
import sifive.fpgashells.ip.xilinx.xxv_ethernet._

class XDMATopPads(val numLanes: Int) extends Bundle {
  val refclk = Input(new LVDSClock)
  val lanes = new XDMAPads(numLanes)
}

class XDMABridge(val numLanes: Int) extends Bundle {
  val lanes = new XDMAPads(numLanes)
  val srstn = Input(Bool())
  val O     = Input(Clock())
  val ODIV2 = Input(Clock())
}

abstract class EthernetUltraScalePlacedOverlay(name: String, di: EthernetDesignInput, si: EthernetShellInput, config: XXVEthernetParams)
  extends EthernetPlacedOverlay(name, di, si)
{
  def shell: UltraScaleShell

  // val pcs = LazyModule(new XXVEthernet(config))
  val pcs = LazyModule(new CMACEthernet())
  pcs.suggestName(name + "_pcs")

  //val padSource = BundleBridgeSource(() => new XXVEthernetPads)
  val padSource = BundleBridgeSource(() => new CMACEthernetPads)
  val padSink = shell { padSource.makeSink() }
  val dclk = InModuleBody { Wire(Clock()) }

  // def overlayOutput = EthernetOverlayOutput(axi = pcs.node, ox = pcs.node2)
  def overlayOutput = EthernetOverlayOutput(ox = pcs.node2, regdata = pcs.node3)

  InModuleBody {
    padSource.bundle <> pcs.module.io.pads

    val clocks = pcs.module.io.clocks
    //clocks.rx_core_clk_0 := clocks.rx_clk_out_0
    //clocks.dclk          := dclk
    //clocks.sys_reset     := Module.reset
    clocks.rx_clk := clocks.gt_txusrclk2
    clocks.drp_clk := dclk
    clocks.init_clk := dclk
    clocks.sys_reset := Module.reset

    // pcs.module.io.axi4lite.s_axi_aclk_0 := Module.clock
    // pcs.module.io.axi4lite.s_axi_aresetn_0 := !Module.reset.asBool

    // refclk_p is added by the IP xdc's anonymous create_clock [get_pins name_refclk_p]
    //shell.sdc.addGroup(clocks = Seq(s"${name}_refclk_p"), pins = Seq(pcs.island.module.blackbox.io.tx_clk_out_0))
    shell.sdc.addGroup(clocks = Seq(s"${name}_refclk_p"), pins = Seq(pcs.island.module.blackbox.io.gt_txusrclk2))
  }

  shell { InModuleBody {
    val pcsIO = padSink.bundle
    io.tx0_p := pcsIO.gt_txp_out(0)
    io.tx0_n := pcsIO.gt_txn_out(0)
    io.tx1_p := pcsIO.gt_txp_out(1)
    io.tx1_n := pcsIO.gt_txn_out(1)
    io.tx2_p := pcsIO.gt_txp_out(2)
    io.tx2_n := pcsIO.gt_txn_out(2)
    io.tx3_p := pcsIO.gt_txp_out(3)
    io.tx3_n := pcsIO.gt_txn_out(3)
    pcsIO.gt_rxp_in := Cat(io.rx3_p, io.rx2_p, io.rx1_p, io.rx0_p)
    pcsIO.gt_rxn_in := Cat(io.rx3_n, io.rx2_n, io.rx1_n, io.rx0_n)
    pcsIO.gt_ref_clk_p := io.refclk_p
    pcsIO.gt_ref_clk_n := io.refclk_n
    pcs.module.io.gpio_sw_s := io.gpio_sw_s
    pcs.module.io.gpio_sw_n := io.gpio_sw_n
    pcs.module.io.gpio_sw_w := io.gpio_sw_w
    pcs.module.io.gpio_sw_e := io.gpio_sw_e
  } }
}

abstract class PCIeUltraScalePlacedOverlay(name: String, di: PCIeDesignInput, si: PCIeShellInput, config: XDMAParams)
  extends PCIePlacedOverlay[XDMATopPads](name, di, si)
{
  def shell: UltraScaleShell

  val pcie      = LazyModule(new XDMA(config))
  val bridge    = BundleBridgeSource(() => new XDMABridge(config.lanes))
  val topBridge = shell { bridge.makeSink() }
  val axiClk    = ClockSourceNode(freqMHz = config.axiMHz)
  val areset    = ClockSinkNode(Seq(ClockSinkParameters()))
  areset := di.wrangler := axiClk

  val slaveSide = TLIdentityNode()
  pcie.crossTLIn(pcie.slave)   := slaveSide
  pcie.crossTLIn(pcie.control) := slaveSide
  val node = NodeHandle(slaveSide, pcie.crossTLOut(pcie.master))
  val intnode = pcie.crossIntOut(pcie.intnode)

  def overlayOutput = PCIeOverlayOutput(node, intnode)
  def ioFactory = new XDMATopPads(config.lanes)

  InModuleBody {
    val (axi, _) = axiClk.out(0)
    val (ar, _) = areset.in(0)
    val b = bridge.out(0)._1

    pcie.module.clock := ar.clock
    pcie.module.reset := ar.reset

    b.lanes <> pcie.module.io.pads

    axi.clock := pcie.module.io.clocks.axi_aclk
    axi.reset := !pcie.module.io.clocks.axi_aresetn
    pcie.module.io.clocks.sys_rst_n  := b.srstn
    pcie.module.io.clocks.sys_clk    := b.ODIV2
    pcie.module.io.clocks.sys_clk_gt := b.O

    shell.sdc.addGroup(clocks = Seq(s"${name}_ref_clk"), pins = Seq(pcie.imp.module.blackbox.io.axi_aclk))
//    shell.sdc.addGroup(pins = Seq(pcie.imp.module.blackbox.io.sys_clk_gt))
    shell.sdc.addAsyncPath(Seq(pcie.imp.module.blackbox.io.axi_aresetn))
  }

  shell { InModuleBody {
    val b = topBridge.in(0)._1

    val ibufds = Module(new IBUFDS_GTE4)
    ibufds.suggestName(s"${name}_refclk_ibufds")
    ibufds.io.CEB := false.B
    ibufds.io.I   := io.refclk.p
    ibufds.io.IB  := io.refclk.n
    b.O     := ibufds.io.O
    b.ODIV2 := ibufds.io.ODIV2
    b.srstn := !shell.pllReset
    io.lanes <> b.lanes

    shell.sdc.addClock(s"${name}_ref_clk", io.refclk.p, 100)
  } }
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
