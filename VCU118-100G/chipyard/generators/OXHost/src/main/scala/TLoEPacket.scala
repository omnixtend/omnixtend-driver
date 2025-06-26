package omnixtend

import chisel3._
import chisel3.util._

/**
 * EthernetHeader class defines the structure of an Ethernet header.
 */
class EthernetHeader extends Bundle {
// val preamble  = UInt(64.W)    // 8-byte Preamble/SFD
  val destMAC   = UInt(48.W)    // 6-byte Destination MAC Address
  val srcMAC    = UInt(48.W)    // 6-byte Source MAC Address
  val etherType = UInt(16.W)    // 2-byte EtherType field
}

/**
 * OmniXtendHeader class defines the structure of an OmniXtend header.
 * 64 Bits (8 Bytes)
 */
class OmniXtendHeader extends Bundle {
  val vc        = UInt(3.W)     // Virtual Channel
  val msgType   = UInt(4.W)     // Reserved
  val res1      = UInt(3.W)     // Reserved
  val seqNum    = UInt(22.W)    // Sequence Number
  val seqNumAck = UInt(22.W)    // Sequence Number Acknowledgment
  val ack       = UInt(1.W)     // Acknowledgment
  val res2      = UInt(1.W)     // Reserved
  val chan      = UInt(3.W)     // Channel
  val credit    = UInt(5.W)     // Credit
}

/**
 * TileLinkMessage class defines the structure of a TileLink message.
 * 64 Bits (8 Bytes)
 */
class TLMessageHigh extends Bundle {
  val res1      = UInt(1.W)     // Reserved
  val chan      = UInt(3.W)     // Channel
  val opcode    = UInt(3.W)     // Opcode
  val res2      = UInt(1.W)     // Reserved
  val param     = UInt(4.W)     // Parameter
  val size      = UInt(4.W)     // Size
  val domain    = UInt(8.W)     // Domain
  val err       = UInt(2.W)     // Error
  val res3      = UInt(12.W)    // Reserved
  val source    = UInt(26.W)    // Source
}

/**
 * TileLinkMessage class defines the structure of a TileLink message.
 */
class TLMessageLow extends Bundle {
  val addr      = UInt(64.W)    // Address
}

/**
 * TloePacket class defines the structure of a TLoE packet.
 */
class TloePacket extends Bundle {
  val ethHeader   = new EthernetHeader
  val omniHeader  = new OmniXtendHeader
  val tlMsgHigh   = new TLMessageHigh
  val tlMsgLow    = new TLMessageLow
}

/**
 * TloePacketGenerator object contains functions to create and manipulate TLoE packets.
 */
object TloePacketGenerator {

  /**
   * Converts a 64-bit unsigned integer from little-endian to big-endian format.
   *
   * In big-endian format, the most significant byte is stored at the smallest address.
   * This function rearranges the bytes of the input value to convert it from 
   * little-endian to big-endian.
   */
  def toBigEndian(value: UInt): UInt = {
    require(value.getWidth == 64, "Input must be 64 bits wide")  // Ensure the input is 64 bits wide

    // Rearrange the bytes of the input value to convert it to big-endian format
    Cat(
      value(7, 0),     // Least significant byte (original bits 7:0)
      value(15, 8),    // Next byte (original bits 15:8)
      value(23, 16),   // Next byte (original bits 23:16)
      value(31, 24),   // Next byte (original bits 31:24)
      value(39, 32),   // Next byte (original bits 39:32)
      value(47, 40),   // Next byte (original bits 47:40)
      value(55, 48),   // Next byte (original bits 55:48)
      value(63, 56)    // Most significant byte (original bits 63:56)
    )
  }

  /**
   * Creates a new TLoE (TileLink over Ethernet) packet.
   *
   * This function constructs a TLoE packet by populating the various fields of the packet
   * with appropriate values. The packet is divided into different headers and message
   * components, such as the Ethernet header, OmniXtend header, and TileLink message fields.
   */
  def createTloePacket(txAddr: UInt, txData: UInt, txOpcode: UInt): UInt = {
    // Create a new instance of the TloePacket (a user-defined bundle)
    val tloePacket = Wire(new TloePacket)

    // Populate the Ethernet header fields
    tloePacket.ethHeader.destMAC    := "h043f72dd0acc".U  // 6-byte Destination MAC Address
    tloePacket.ethHeader.srcMAC     := "h123456789ABC".U  // 6-byte Source MAC Address
    tloePacket.ethHeader.etherType  := "hAAAA".U          // 2-byte EtherType (Example value for TLoE)

    // Populate the OmniXtend header fields
    tloePacket.omniHeader.vc        := 0.U  // Virtual Channel ID
    tloePacket.omniHeader.res1      := 0.U  // Reserved field 1
    tloePacket.omniHeader.seqNum    := 0.U  // Sequence Number
    tloePacket.omniHeader.seqNumAck := 0.U  // Acknowledged Sequence Number
    tloePacket.omniHeader.ack       := 1.U  // Acknowledgment flag
    tloePacket.omniHeader.res2      := 0.U  // Reserved field 2
    tloePacket.omniHeader.credit    := 0.U  // Credit field
    tloePacket.omniHeader.chan      := 0.U  // Channel ID

    // Populate the high part of the TileLink message fields
    tloePacket.tlMsgHigh.res1       := 0.U      // Reserved field 1
    tloePacket.tlMsgHigh.chan       := 0.U      // Channel ID
    tloePacket.tlMsgHigh.opcode     := txOpcode // TileLink operation code (input parameter)
    tloePacket.tlMsgHigh.res2       := 0.U      // Reserved field 2
    tloePacket.tlMsgHigh.param      := 0.U      // TileLink parameter field
    tloePacket.tlMsgHigh.size       := 0.U      // Size of the transaction
    tloePacket.tlMsgHigh.domain     := 0.U      // Domain field
    tloePacket.tlMsgHigh.err        := 0.U      // Error field
    tloePacket.tlMsgHigh.res3       := 0.U      // Reserved field 3
    tloePacket.tlMsgHigh.source     := 0.U      // Source field

    // Populate the low part of the TileLink message fields
    tloePacket.tlMsgLow.addr    := txAddr   // TileLink address (input parameter)

    // Convert the TLoE packet bundle to a single UInt representing the entire packet
    tloePacket.asUInt
  }

    def getEtherType(packet: Vec[UInt]): UInt = {
    val seqNum = toBigEndian(packet(1))(31, 16)
    seqNum
  }

  // seqNum 추출 함수
  def getSeqNum(packet: Vec[UInt]): UInt = {
    val seqNum = Cat(toBigEndian(packet(1))(5, 0), toBigEndian(packet(2))(63, 48)) // Vec(1)의 상위 32비트 + Vec(2)의 상위 10비트
    seqNum
  }

  // seqNumAck 추출 함수
  def getSeqNumAck(packet: Vec[UInt]): UInt = {
    val seqNumAck = toBigEndian(packet(2))(47, 26) // Vec(2)의 53~32번째 비트
    seqNumAck
  }

  // chan 추출 함수
  def getChan(packet: Vec[UInt]): UInt = {
    val chan = toBigEndian(packet(2))(23, 21) // Vec(2)의 9~7번째 비트가 Chan
    chan
  }

  // credit 추출 함수
  def getCredit(packet: Vec[UInt]): UInt = {
    val credit = toBigEndian(packet(2))(20, 16) // Vec(2)의 6~2번째 비트가 Credit
    credit
  }
}