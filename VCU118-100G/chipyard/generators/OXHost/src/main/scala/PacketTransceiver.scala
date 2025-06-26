package omnixtend

import chisel3._
import chisel3.util._

/**
 * Transceiver module interfaces with the TileLink messages and manages the
 * serialization and deserialization of data for transmission and reception.
 * This module acts as a bridge between TileLink and Ethernet, handling both
 * transmission and reception of data packets.
 */
class Transceiver extends Module {
  val io = IO(new Bundle {
    // TileLink interface for transmission
    val txAddr      = Input(UInt(64.W))   // Input address for transmission
    val txData      = Input(UInt(64.W))   // Input data for transmission
    val txSize      = Input(UInt(3.W))    // Input data for transmission
    val txOpcode    = Input(UInt(3.W))    // Input opcode for transmission
    val txValid     = Input(Bool())       // Valid signal for transmission
    val txReady     = Output(Bool())      // Ready signal for transmission

    // TileLink interface for reception
    val rxData      = Output(UInt(64.W))  // Output data received
    val rxValid     = Output(Bool())      // Valid signal for received data
    val rxReady     = Input(Bool())       // Ready signal for receiver

    // Ethernet IP core interface
    val axi_rxdata  = Output(UInt(64.W))
    val axi_rxvalid = Output(Bool())
    val txdata      = Output(UInt(64.W))
    val txvalid     = Output(Bool())
    val txlast      = Output(Bool())
    val txkeep      = Output(UInt(8.W))
    val txready     = Input(Bool())
    val rxdata      = Input(UInt(64.W))
    val rxvalid     = Input(Bool())
    val rxlast      = Input(Bool())

    val ox_open     = Input(Bool())
    val ox_close    = Input(Bool())
    val debug1   = Input(Bool())
    val debug2   = Input(Bool())
  })

  val conn    = RegInit(0.U(8.W))

  val A_channel = RegInit(0.U(16.W))  // 0~65535
  val C_channel = RegInit(0.U(16.W))  // 0~65535
  val E_channel = RegInit(0.U(16.W))  // 0~65535

  // Configuration for packet replication cycles
  val replicationCycles = 10

  // TX AXI-Stream to Tilelink (Transmission Path)
  val axi_txdata  = RegInit(0.U(64.W))
  val axi_txvalid = RegInit(false.B)
  val axi_txlast  = RegInit(false.B)
  val axi_txkeep  = RegInit(0.U(8.W))

  val next_tx_seq = RegInit(0.U(22.W))
  val ackd_seq    = RegInit("h3FFFFF".U(22.W))
  val next_rx_seq = RegInit(0.U(22.W))

  val oPacket     = RegInit(0.U(576.W))

  // Registers to hold the AXI-Stream signals
  val axi_rxdata  = RegInit(0.U(64.W))
  val axi_rxvalid = RegInit(false.B)

  val rxcount     = RegInit(0.U(8.W))
  val rPacketVec  = RegInit(VecInit(Seq.fill(9)(0.U(64.W))))
  val rPacketVecSize  = RegInit(0.U(4.W))

  val txPacketVec = RegInit(VecInit(Seq.fill(9)(0.U(64.W))))
  val txPacketVecSize = RegInit(0.U(4.W))  // 0~15

  val rxPacketReceived = RegInit(false.B)

  // Connecting internal signals to output interface
  io.txvalid := axi_txvalid
  io.txdata := axi_txdata
  io.txlast := axi_txlast
  io.txkeep := axi_txkeep

  // Connecting internal signals to output interface
  io.axi_rxdata  := axi_rxdata
  io.axi_rxvalid := axi_rxvalid

  //////////////////////////////////////////////////////////////////
  // State Machines
  //val oidle :: opacket1_ready :: opacket1_sent :: opacket2_ready :: opacket2_sent :: owaiting_ack1 :: owaiting_ack2 :: osending_ack :: owaiting_ack3 :: Nil = Enum(9)
  val oidle :: opacket1_ready :: opacket1_sent :: opacket2_ready :: opacket2_sent :: opacket3_ready :: opacket3_sent :: opacket4_ready :: opacket4_sent :: opacket5_ready :: opacket5_sent :: owaiting_ack1 :: owaiting_ack2 :: osending_ack :: owaiting_ack3 :: owaiting_ack4 :: Nil = Enum(16)
  val state = RegInit(oidle)

  val cidle :: cpacket_sent :: cwaiting_ack :: Nil = Enum(3)
  val cstate = RegInit(cidle)

  val ridle :: rsendRequest :: rwaitCredit :: rwaitResponse :: rprocessResponse :: rwaitAck :: rdone :: Nil = Enum(7)
  val rstate = RegInit(ridle)

  val widle :: wsendRequest :: wwaitCredit1 :: wwaitCredit2 :: wwaitResponse :: wprocessResponse :: wwaitAck :: wdone :: Nil = Enum(8)
  val wstate = RegInit(ridle)

  val idx = RegInit(0.U(16.W))  // 패킷 인덱스 저장 레지스터

  //////////////////////////////////////////////////////////////////
  // Senging Packet
  val sendPacket = RegInit(false.B)
  val txComplete = RegInit(false.B)

  when (sendPacket) {
    when (idx < txPacketVecSize) {
      // 현재 인덱스에 해당하는 패킷을 axi_txdata에 저장
      axi_txdata := TloePacketGenerator.toBigEndian(txPacketVec(idx))
      axi_txvalid := true.B
      idx := idx + 1.U      // 다음 인덱스로 이동

      // 마지막 패킷인지 확인
      when (idx === (txPacketVecSize - 1.U)) {
        axi_txlast := true.B
        axi_txkeep := 0x3F.U  // 마지막 패킷 신호
        idx := 20.U
      } .otherwise {
        axi_txlast := false.B
        axi_txkeep := 0xFF.U  // 일반 패킷 신호
      }
    }.otherwise {
      axi_txdata := 0.U
      axi_txvalid := false.B
      axi_txlast := false.B
      axi_txkeep := 0.U

      idx := 0.U
      sendPacket := false.B
      txComplete := true.B  // 전송 완료 플래그 설정
    }
  }

  //////////////////////////////////////////////////////////////////
  // Connect
  when (io.ox_open) {
    when (state === oidle) {
      state := opacket1_ready
      //oPacket := OXPacket.openConnection(next_tx_seq+1.U, 2.U, 9.U)  // Credit 9
      oPacket := OXPacket.openConnection(next_tx_seq, 1.U, 9.U)  // Credit 9
    }
  }

  when (state === opacket1_ready) {
    txPacketVec := VecInit(Seq.tabulate(9) { i =>
      val packetWidth = 576
      val high = packetWidth - (64 * i) - 1
      val low = math.max(packetWidth - 64 * (i + 1), 0)
      oPacket(high, low)
    })
    next_tx_seq := next_tx_seq + 1.U

    txPacketVecSize := 9.U

    sendPacket := true.B
    txComplete := false.B
    state := opacket1_sent
  }

  when (state === opacket1_sent) {
    when (txComplete) {
      state := opacket2_ready
      //oPacket := OXPacket.openConnection(next_tx_seq+1.U, 4.U, 9.U)  // Credit 9
      oPacket := OXPacket.openConnection(next_tx_seq, 2.U, 9.U)  // Credit 9

      txComplete := false.B
    }
  }

  when (state === opacket2_ready) {
    txPacketVec := VecInit(Seq.tabulate(9) { i =>
      val packetWidth = 576
      val high = packetWidth - (64 * i) - 1
      val low = math.max(packetWidth - 64 * (i + 1), 0)
      oPacket(high, low)
    })
    next_tx_seq := next_tx_seq + 1.U

    txPacketVecSize := 9.U

    sendPacket := true.B
    txComplete := false.B
    state := opacket2_sent
  }

  when (state === opacket2_sent) {
    when (txComplete) {
      state := opacket3_ready
      //oPacket := OXPacket.openConnection(next_tx_seq+1.U, 4.U, 9.U)  // Credit 9
      oPacket := OXPacket.openConnection(next_tx_seq, 3.U, 9.U)  // Credit 9

      txComplete := false.B
    }
  }

  when (state === opacket3_ready) {
    txPacketVec := VecInit(Seq.tabulate(9) { i =>
      val packetWidth = 576
      val high = packetWidth - (64 * i) - 1
      val low = math.max(packetWidth - 64 * (i + 1), 0)
      oPacket(high, low)
    })
    next_tx_seq := next_tx_seq + 1.U

    txPacketVecSize := 9.U

    sendPacket := true.B
    txComplete := false.B
    state := opacket3_sent
  }

  when (state === opacket3_sent) {
    when (txComplete) {
      state := opacket4_ready
      oPacket := OXPacket.openConnection(next_tx_seq, 4.U, 9.U)  // Credit 9

      txComplete := false.B
    }
  }

  when (state === opacket4_ready) {
    txPacketVec := VecInit(Seq.tabulate(9) { i =>
      val packetWidth = 576
      val high = packetWidth - (64 * i) - 1
      val low = math.max(packetWidth - 64 * (i + 1), 0)
      oPacket(high, low)
    })
    next_tx_seq := next_tx_seq + 1.U

    txPacketVecSize := 9.U

    sendPacket := true.B
    txComplete := false.B
    state := opacket4_sent
  }

  when (state === opacket4_sent) {
    when (txComplete) {
      state := opacket5_ready
      //oPacket := OXPacket.openConnection(next_tx_seq+1.U, 4.U, 9.U)  // Credit 9
      oPacket := OXPacket.openConnection(next_tx_seq, 5.U, 9.U)  // Credit 9

      txComplete := false.B
    }
  }

  when (state === opacket5_ready) {
    txPacketVec := VecInit(Seq.tabulate(9) { i =>
      val packetWidth = 576
      val high = packetWidth - (64 * i) - 1
      val low = math.max(packetWidth - 64 * (i + 1), 0)
      oPacket(high, low)
    })
    next_tx_seq := next_tx_seq + 1.U

    txPacketVecSize := 9.U

    sendPacket := true.B
    txComplete := false.B
    state := opacket5_sent
  }

  when (state === opacket5_sent) {
    when (txComplete) {
      state := owaiting_ack1

      txComplete := false.B
    }
  }


/* test
  when (state === opacket2_sent) {
    when (txComplete) {
      state := owaiting_ack1
      txComplete := false.B

//      cstate := cready
    }
  }
*/

/*
  when (cstate === crunning) {
    // if normal packet
    switch((TloePacketGenerator.toBigEndian(rPacketVec(3)))(23, 21)) {
      is(1.U) {
        A_channel := (TloePacketGenerator.toBigEndian(rPacketVec(3)))(20, 16)
        next_rx_seq := next_rx_seq + 1.U // TODO from rx packet??
      }
      is(3.U) {
        C_channel := (TloePacketGenerator.toBigEndian(rPacketVec(3)))(20, 16) 
        next_rx_seq := next_rx_seq + 1.U // TODO from rx packet??
      }
      is(5.U) {
        E_channel := (TloePacketGenerator.toBigEndian(rPacketVec(3)))(20, 16) 
        next_rx_seq := next_rx_seq + 1.U // TODO from rx packet??
      }
    }

    // TODO A, C, E check??? 
    when (A_channel =/= 0.U && C_channel =/= 0.U && E_channel =/= 0.U) {

    }
  }
*/

  // if Ackonly Packet
  val rx_seq_num = RegInit(1.U(2.W))

  when (state === owaiting_ack1) {
    //TODO handling recv packet
    when (rx_seq_num === 1.U) {
      state := owaiting_ack2
      rx_seq_num := 2.U

      //TODO Not here
      ackd_seq := 2.U
    }
  }

  when (state === owaiting_ack2) {
    //TODO handling recv packet
    when (rx_seq_num === 2.U) {
      state := osending_ack
      rx_seq_num := 3.U

      oPacket := OXPacket.normalAck(next_tx_seq, ackd_seq, 1.U, 0.U, 0.U)  //TODO ack number
    }
  }

  when (state === osending_ack) {

    txPacketVec := VecInit(Seq.tabulate(9) { i =>
      val packetWidth = 576
      val high = packetWidth - (64 * i) - 1
      val low = math.max(packetWidth - 64 * (i + 1), 0)
      oPacket(high, low)
    })
    next_tx_seq := next_tx_seq + 1.U

    txPacketVecSize := 9.U

    sendPacket := true.B
    txComplete := false.B
    state := owaiting_ack3
  }

  when (state === owaiting_ack3 && rx_seq_num === 3.U) {
    //TODO handling recv packet
    when (txComplete && rx_seq_num === 3.U) {
      state := oidle

      //TODO not here
      next_tx_seq := 6.U
      ackd_seq    := 5.U
      next_rx_seq := 3.U
    }
  }

  //////////////////////////////////////////////////////////////////
  // Close Connection
  val cPacket     = RegInit(0.U(576.W))

  when (io.ox_close) {
    cstate := cpacket_sent 
    cPacket := OXPacket.closeConnection(next_tx_seq +1.U)
  }

  switch (cstate) {
    is (cpacket_sent) {
      txPacketVec := VecInit(Seq.tabulate(9) { i =>
        val packetWidth = 576
        val high = packetWidth - (64 * i) - 1
        val low = math.max(packetWidth - 64 * (i + 1), 0)
        cPacket(high, low)
      })
      next_tx_seq := next_tx_seq + 1.U

      txPacketVecSize := 9.U

      sendPacket := true.B
      txComplete := false.B
      cstate := cwaiting_ack
    }

    is (cwaiting_ack) {
      //TODO handling recv packet
      when (txComplete) {
        cstate := cidle

        txComplete := false.B
      }
    }
  }

  //////////////////////////////////////////////////////////////////
  // RX

  when (!io.rxvalid) {
    rxcount := 0.U
  }
 
  when (io.rxvalid) {
    rxcount := rxcount + 1.U

    rPacketVec(rxcount) := io.rxdata

      when (next_rx_seq === TloePacketGenerator.getSeqNum(rPacketVec)) {
        when (rstate === rwaitCredit || rstate === rwaitResponse || rstate === rwaitAck) {
          rxPacketReceived := true.B
        }.elsewhen (wstate === wwaitCredit1 || wstate === wwaitCredit2 || wstate === wwaitResponse || wstate === wwaitAck) {
          rxPacketReceived := true.B
        }.elsewhen (state === owaiting_ack1 || state === owaiting_ack2 || state === owaiting_ack3 || state === owaiting_ack4) {
          rxPacketReceived := true.B
        }.otherwise {
        }

        rPacketVecSize := rxcount
      }
/*
      when (cstate === cready) {
        cstate := crunning
      }.elsewhen (rstate === waitResponse || rstate === waitAck) {
      }.elsewhen (wstate === waitResponse || wstate === waitAck) {
        rxPacketReceived := true.B
      }
*/

      rxcount := 0.U
    }
  

  //////////////////////////////////////////////////////////////////
  // TX

  val rPacket     = RegInit(0.U(576.W))
  val wPacket     = RegInit(0.U(576.W))

  rPacket := 0.U

  // TX AXI-Stream data/valid/last
  when (io.txValid) {

    when (io.txOpcode === 4.U) {		// READ
      rPacket := OXPacket.readPacket(io.txAddr, next_tx_seq, next_rx_seq)
      rstate := rsendRequest
    }.elsewhen (io.txOpcode === 0.U) {	// WRITE
      wPacket := OXPacket.writePacket(io.txAddr, io.txData, next_tx_seq, next_rx_seq)
      wstate := wsendRequest
    }.otherwise {
      //TODO
    }
  }

  //////////////////////////////////////////////////////////////////
  // READ

  switch (rstate) {
    is (rsendRequest) {
      // make packet
      txPacketVec := VecInit(Seq.tabulate(9) (i => {
        val packetWidth = OXPacket.readPacket(io.txAddr, next_tx_seq, next_rx_seq).getWidth
        val high = packetWidth - (64 * i) - 1
	    val low = math.max(packetWidth - 64 * (i + 1), 0)

        rPacket (high, low)
      }))
      next_tx_seq := next_tx_seq + 1.U

      txPacketVecSize := 9.U

      sendPacket := true.B
      txComplete := false.B

      rstate := rwaitCredit
    }

    is (rwaitCredit) {
      when (txComplete && rxPacketReceived) {
        // Update acked_seq TODO If not drop

        ackd_seq := TloePacketGenerator.toBigEndian(rPacketVec(2))(47, 26)   // seqNumAck
        next_rx_seq := Cat(
          (TloePacketGenerator.toBigEndian(rPacketVec(1)))(5, 0), 
          (TloePacketGenerator.toBigEndian(rPacketVec(2)))(63, 48)
        ) + 1.U 

        // TODO Do something with Channel A

        rxPacketReceived := false.B

        txComplete := false.B
        rstate := rwaitResponse
      }
    }

    is (rwaitResponse) {
      when (rxPacketReceived) {

        // Update acked_seq TODO If not drop
        ackd_seq := TloePacketGenerator.toBigEndian(rPacketVec(2))(47, 26)   // seqNumAck
        next_rx_seq := Cat(
          (TloePacketGenerator.toBigEndian(rPacketVec(1)))(5, 0), 
          (TloePacketGenerator.toBigEndian(rPacketVec(2)))(63, 48)
        ) + 1.U 

        // Return through TL message
        axi_rxdata := Cat(
          (TloePacketGenerator.toBigEndian(rPacketVec(3)))(15, 0), 
          (TloePacketGenerator.toBigEndian(rPacketVec(4)))(63, 16)
        ) 
        axi_rxvalid := true.B

        rxPacketReceived := false.B

        // Make Normal message
        rPacket := OXPacket.normalAck(next_tx_seq, next_rx_seq + 1.U, 1.U, 4.U, 1.U)  //TODO ack number
        rstate := rprocessResponse
      }
    }

    is (rprocessResponse) {
      axi_rxvalid := false.B
      // Change packet to Vec
      txPacketVec := VecInit(Seq.tabulate(9) (i => {
        val packetWidth = OXPacket.readPacket(io.txAddr, next_tx_seq, ackd_seq).getWidth
        val high = packetWidth - (64 * i) - 1
	    val low = math.max(packetWidth - 64 * (i + 1), 0)

        rPacket (high, low)
      }))
      next_tx_seq := next_tx_seq + 1.U

      txPacketVecSize := 9.U

      // Send packet
      sendPacket := true.B
      txComplete := false.B

      rstate := rwaitAck
    }

    is (rwaitAck) {

      when (txComplete && rxPacketReceived) {
        // Update acked_seq TODO If not drop

        ackd_seq := TloePacketGenerator.toBigEndian(rPacketVec(2))(47, 26)   // seqNumAck
/*
        next_rx_seq := Cat(
          (TloePacketGenerator.toBigEndian(rPacketVec(1)))(5, 0), 
          (TloePacketGenerator.toBigEndian(rPacketVec(2)))(63, 48)
        ) + 1.U      // seqNum
*/

        // TODO check exptected Ack
        rstate := rdone
        rxPacketReceived := false.B
        txComplete := false.B

      }
    }

    is (rdone) {
      rstate := ridle
    }
  }

  //////////////////////////////////////////////////////////////////
  // WRITE

  switch (wstate) {
    is (wsendRequest) {
      // make packet
      txPacketVec := VecInit(Seq.tabulate(9) (i => {
        val packetWidth = OXPacket.readPacket(io.txAddr, next_tx_seq, ackd_seq).getWidth
        val high = packetWidth - (64 * i) - 1
	    val low = math.max(packetWidth - 64 * (i + 1), 0)

        wPacket (high, low)
      }))
      next_tx_seq := next_tx_seq + 1.U

      txPacketVecSize := 9.U

      sendPacket := true.B
      txComplete := false.B

      wstate := wwaitCredit1
    }

    is (wwaitCredit1) {
      when (rxPacketReceived) {
        // Update seq numbers
        ackd_seq := TloePacketGenerator.toBigEndian(rPacketVec(2))(47, 26)   // seqNumAck
        next_rx_seq := Cat(
          (TloePacketGenerator.toBigEndian(rPacketVec(1)))(5, 0), 
          (TloePacketGenerator.toBigEndian(rPacketVec(2)))(63, 48)
        ) + 1.U 

       // TODO Update credits of A channel

        rxPacketReceived := false.B

        wstate := wwaitCredit2
      }
    }

    is (wwaitCredit2) {
      when (rxPacketReceived) {
        // Update seq numbers
        ackd_seq := TloePacketGenerator.toBigEndian(rPacketVec(2))(47, 26)   // seqNumAck
        next_rx_seq := Cat(
          (TloePacketGenerator.toBigEndian(rPacketVec(1)))(5, 0), 
          (TloePacketGenerator.toBigEndian(rPacketVec(2)))(63, 48)
        ) + 1.U 

       // TODO Update credits of A channel

      // TODO If TileLink packet, handle
        when (Cat(
          TloePacketGenerator.toBigEndian(rPacketVec(rPacketVecSize-1.U))(15, 0),
          TloePacketGenerator.toBigEndian(rPacketVec(rPacketVecSize))(63, 16)) =/= 0.U) {
            wPacket := OXPacket.normalAck(next_tx_seq, next_rx_seq + 1.U, 1.U, 4.U, 4.U)  //TODO ack number

            axi_rxdata := 0.U
            axi_rxvalid := true.B

            rPacketVecSize := 0.U

            wstate := wprocessResponse
          }.otherwise {
            wstate := wwaitResponse
          }
          rxPacketReceived := false.B
        }
      }

    is (wwaitResponse) {
      when (txComplete && rxPacketReceived) {
        // Update acked_seq TODO If not drop
        ackd_seq := TloePacketGenerator.toBigEndian(rPacketVec(2))(47, 26)   // seqNumAck
        next_rx_seq := Cat(
          (TloePacketGenerator.toBigEndian(rPacketVec(1)))(5, 0), 
          (TloePacketGenerator.toBigEndian(rPacketVec(2)))(63, 48)
        ) + 1.U 

        // TODO
        axi_rxdata := 12345.U
        axi_rxvalid := true.B

        rxPacketReceived := false.B
        txComplete := false.B

        wPacket := OXPacket.normalAck(next_tx_seq, next_rx_seq + 1.U, 1.U, 4.U, 1.U)  //TODO ack number
        wstate := wprocessResponse
      }
    }

    is (wprocessResponse) {
      axi_rxvalid := false.B
      // make packet
      txPacketVec := VecInit(Seq.tabulate(9) (i => {
        val packetWidth = OXPacket.readPacket(io.txAddr, next_tx_seq, ackd_seq).getWidth
        val high = packetWidth - (64 * i) - 1
	    val low = math.max(packetWidth - 64 * (i + 1), 0)

        wPacket (high, low)
      }))
      next_tx_seq := next_tx_seq + 1.U

      txPacketVecSize := 9.U

      sendPacket := true.B
      txComplete := false.B

      wstate := wwaitAck
    }

    is (wwaitAck) {
      when (txComplete && rxPacketReceived) {
        // Update acked_seq TODO If not drop

        ackd_seq := TloePacketGenerator.toBigEndian(rPacketVec(2))(47, 26)   // seqNumAck
/*
        next_rx_seq := Cat(
          (TloePacketGenerator.toBigEndian(rPacketVec(1)))(5, 0), 
          (TloePacketGenerator.toBigEndian(rPacketVec(2)))(63, 48)
        ) + 1.U      // seqNum
*/

        // TODO check exptected Ack

        wstate := wdone
        rxPacketReceived := false.B
        txComplete := false.B
      }
    }

    is (wdone) {
      wstate := ridle
    }
  }

  /////////////////////////////////////////////////
  /////////////////////////////////////////////////
  // Connect to Simulator with Endpoint module

  // Queue for outgoing TLoE packets
  // 16 entries of TloePacket type
  //val txQueue = Module(new Queue(UInt(640.W), 16))
  val txQueue = Module(new Queue(UInt(64.W), 16))
  
  // Queue for incoming TLoE packets
  // 16 entries of TloePacket type
  val rxQueue = Module(new Queue(UInt(640.W), 16))

  val tx_packet_vec = RegInit(VecInit(Seq.fill(9)(0.U(64.W))))
  val txCount     = RegInit(0.U(log2Ceil(replicationCycles).W))

  val tmpP = RegInit(0.U(576.W))
  val tloePacket = RegInit(2.U(576.W))

  // Default values for transmission queue
  txQueue.io.enq.bits  := 0.U
  txQueue.io.enq.valid := false.B
  io.txReady           := false.B

  // Default values for reception queue
  rxQueue.io.enq.bits  := 0.U
  rxQueue.io.enq.valid := false.B
  io.rxData            := 0.U
  io.rxValid           := false.B

  val txAddrReg = RegNext(io.txAddr)

  // Enqueue a TLoE packet into the transmission queue when txValid is asserted
  when (io.txValid) {

    // Create a TLoE packet using input address, data, and opcode
    tloePacket := OXPacket.readPacket(txAddrReg, next_tx_seq, ackd_seq)

    tx_packet_vec := VecInit(Seq.tabulate(9) (i => {
      //val packetWidth = OXPacket.createOXPacket(io.txAddr, next_tx_seq, ackd_seq).getWidth
      val packetWidth = 576
      val high = packetWidth - (64 * i) - 1
      val low = math.max(packetWidth - 64 * (i + 1), 0)

      tloePacket (high, low)
    }))

    //tmpP := tloePacket
    txQueue.io.enq.bits := txAddrReg
    txCount := 1.U
  }

  // Enqueue the TLoE packet into txQueue when txValid is high
  when(txCount > 0.U && txCount < replicationCycles.U) {
    txQueue.io.enq.bits := txPacketVec(txCount - 1.U)
    txCount := txCount + 1.U
  } .elsewhen (txCount === replicationCycles.U) {
    txQueue.io.enq.valid := 1.U
    txCount := txCount + 1.U
  } .elsewhen (txCount === (replicationCycles + 1).U) {
    // Reset signals after transmission
    txCount := 0.U
  }
 
  txQueue.io.enq.valid := io.txValid
  io.txReady           := txQueue.io.enq.ready

  // Instantiate the Endpoint module
  val endpoint = Module(new Endpoint)

  // Connect txQueue to the Endpoint for transmission
  endpoint.io.txQueueData.bits  := txQueue.io.deq.bits
  endpoint.io.txQueueData.valid := txQueue.io.deq.valid
  txQueue.io.deq.ready          := endpoint.io.txQueueData.ready

  // Handle rxQueue and deserialize data
  endpoint.io.rxQueueData.ready := rxQueue.io.enq.ready

  // Enqueue received data into rxQueue when Endpoint valid signal is high
  when(endpoint.io.rxQueueData.valid) {
    rxQueue.io.enq.bits  := endpoint.io.rxQueueData.bits
    rxQueue.io.enq.valid := true.B
  }

  // Dequeue data from rxQueue and output when rxReady is high
  when(rxQueue.io.deq.valid && io.rxReady) {
    val rxPacket = rxQueue.io.deq.bits
    io.rxData            := rxPacket
    io.rxValid           := true.B
    rxQueue.io.deq.ready := io.rxReady
  }.otherwise {
    io.rxValid           := false.B
    rxQueue.io.deq.ready := false.B
  }
}
