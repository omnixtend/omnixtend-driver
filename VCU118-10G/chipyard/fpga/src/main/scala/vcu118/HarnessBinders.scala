package chipyard.fpga.vcu118

import chisel3._
import chisel3.experimental.{BaseModule}

import freechips.rocketchip.util.{HeterogeneousBag}
import freechips.rocketchip.tilelink.{TLBundle}

import sifive.blocks.devices.uart.{HasPeripheryUARTModuleImp, UARTPortIO}
import sifive.blocks.devices.spi.{HasPeripherySPI, SPIPortIO}

import chipyard.{HasHarnessSignalReferences, CanHaveMasterTLMemPort, CanHaveMasterTLOXPort}
import chipyard.harness.{OverrideHarnessBinder}

import sifive.fpgashells.devices.xilinx.ethernet._
import sifive.fpgashells.shell._
import freechips.rocketchip.subsystem._
import testchipip._
import freechips.rocketchip.amba.axi4._

/*** UART ***/
class WithUART extends OverrideHarnessBinder({
  (system: HasPeripheryUARTModuleImp, th: BaseModule with HasHarnessSignalReferences, ports: Seq[UARTPortIO]) => {
    th match { case vcu118th: VCU118FPGATestHarnessImp => {
      vcu118th.vcu118Outer.io_uart_bb.bundle <> ports.head
    } }
  }
})

/*** SPI ***/
class WithSPISDCard extends OverrideHarnessBinder({
  (system: HasPeripherySPI, th: BaseModule with HasHarnessSignalReferences, ports: Seq[SPIPortIO]) => {
    th match { case vcu118th: VCU118FPGATestHarnessImp => {
      vcu118th.vcu118Outer.io_spi_bb.bundle <> ports.head
    } }
  }
})

/*** Experimental DDR ***/
class WithDDRMem extends OverrideHarnessBinder({
  (system: CanHaveMasterTLMemPort, th: BaseModule with HasHarnessSignalReferences, ports: Seq[HeterogeneousBag[TLBundle]]) => {
    th match { case vcu118th: VCU118FPGATestHarnessImp => {
      require(ports.size == 1)

      val bundles = vcu118th.vcu118Outer.ddrClient.out.map(_._1)
      val ddrClientBundle = Wire(new HeterogeneousBag(bundles.map(_.cloneType)))
      bundles.zip(ddrClientBundle).foreach { case (bundle, io) => bundle <> io }
      ddrClientBundle <> ports.head
    } }
  }
})

/*** Ethernet AXI-Lite***/
class WithEthernetAXI4Lite extends OverrideHarnessBinder({
  (system: CanHaveMasterAXI4MMIOPort, th: BaseModule with HasHarnessSignalReferences, ports: Seq[HeterogeneousBag[AXI4Bundle]]) => {
    println(s"EthernetPorts: ${ports.mkString(", ")}")
    th match { case vcu118th: VCU118FPGATestHarnessImp => {
      val bundles = vcu118th.vcu118Outer.ethClient.out.map(_._1)
      val ethClientBundle = Wire(new HeterogeneousBag(bundles.map(_.cloneType)))
      bundles.zip(ethClientBundle).foreach { case (bundle, io) => bundle <> io }
      ethClientBundle <> ports.head
    } }
  }
})

/*** OX Tilelink ***/
class WithOXTilelink extends OverrideHarnessBinder({
  (system: CanHaveMasterTLOXPort, th: BaseModule with HasHarnessSignalReferences, ports: Seq[HeterogeneousBag[TLBundle]]) => {
    println(s"OXPorts: ${ports.mkString(", ")}")
    th match { case vcu118th: VCU118FPGATestHarnessImp => {
      val bundles = vcu118th.vcu118Outer.oxClient.out.map(_._1)
      val oxClientBundle = Wire(new HeterogeneousBag(bundles.map(_.cloneType)))
      bundles.zip(oxClientBundle).foreach { case (bundle, io) => bundle <> io }
      oxClientBundle <> ports.head
    } }
  }
})