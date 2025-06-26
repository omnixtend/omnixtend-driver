# OmniXtend Driver - Host Node Implementation

A comprehensive implementation of OmniXtend protocol support for Chipyard-based systems, providing 10G and 100G Ethernet connectivity with TileLink over Ethernet (TLoE) capabilities.

![BlockDiagram](docs/images/OmniXtend_over_Ethernet_Block_Diagram.png)

## Overview

This project implements OmniXtend protocol support across multiple FPGA platforms, enabling high-performance communication between Rocket Chip-based systems and external endpoints via Ethernet. The implementation supports both 10G and 100G Ethernet interfaces with comprehensive packet processing capabilities.

## Supported Platforms

### 1. Arty A7-100T (arty100t/)
- **Target**: Xilinx Artix-7 FPGA
- **Ethernet**: 1G Ethernet via AXI Ethernet IP
- **Features**: 
  - Basic OmniXtend protocol support
  - TileLink to Ethernet packet conversion
  - Memory-mapped I/O interface
  - Single Rocket core configuration

### 2. VCU118 10G (VCU118-10G/)
- **Target**: Xilinx Virtex UltraScale+ FPGA
- **Ethernet**: 10G Ethernet via XXV Ethernet IP
- **Features**:
  - High-performance 10G packet processing
  - Advanced packet generation and testing
  - Throughput and latency measurement capabilities
  - VIO (Virtual Input/Output) for runtime control

### 3. VCU118 100G (VCU118-100G/)
- **Target**: Xilinx Virtex UltraScale+ FPGA  
- **Ethernet**: 100G Ethernet via CMAC IP
- **Features**:
  - Ultra-high performance 100G packet processing
  - Advanced packet generator with throughput testing
  - Comprehensive latency measurement
  - Enhanced debugging and monitoring capabilities

## Architecture

### 1.1 Rocket Core Integration
The **Rocket Core** serves as the primary processor and interfaces with external components through multiple bus interfaces:
- **SystemBus**: Main system interconnect
- **MemoryBus**: Memory access interface  
- **FrontBus**: Front-end peripheral interface
- **PeripheryBus**: Peripheral device interface

### 1.2 Ethernet Subsystem
The **Ethernet Subsystem** handles high-speed data transmission and reception with external endpoints, comprising:
- **OmniXtend Module**: Core protocol implementation
- **Packet Generator**: Test packet generation for performance evaluation
- **Packet Transceiver**: TileLink to Ethernet packet conversion
- **Ethernet IP**: Xilinx Ethernet Subsystem IP integration

## Code Structure

### 2.1 Ethernet Subsystem Implementation

#### 2.1.1 xxv_ethernet.scala / cmac_ethernet.scala
Core Ethernet IP integration files that connect the Ethernet Subsystem IP with the OmniXtend module.

**Key Components:**
- `HasXXVEthernetPads`: Physical interface declarations
- `HasXXVEthernetMAC`: MAC layer interface
- `HasXXVEthernetClocks`: Clock domain management
- `HasXXVEthernetAXI4Lite`: AXI-Lite control interface

**Integration Features:**
- `XXVEthernetBlackBox`: Vivado TCL script-based IP generation
- `AXI4StreamFIFO`: Clock domain crossing between OmniXtend and Ethernet IP
- `DiplomaticXXVEthernet`: Diplomatic node creation for AXI-Lite bus
- `XXVEthernet`: Complete integration module with OmniXtendNode

#### 2.1.2 UltraScaleShell.scala
Shell implementation for UltraScale+ FPGAs enabling Ethernet Subsystem IP instantiation in the test harness.

**Key Features:**
- `EthernetUltraScalePlacedOverlay`: Overlay for Ethernet IP placement
- GPIO button input support for runtime control
- Proper node connection management between XXVEthernet and OmniXtendNode

#### 2.1.3 Ethernet.scala
Defines the Ethernet overlay output types and GPIO interface.

**Interface Definitions:**
- `EthernetPlacedOverlayOutput`: Type definitions for AXI and TileLink nodes
- `EthernetPads`: Physical interface with additional GPIO controls

#### 2.1.4 TestHarness.scala
Top-level file generation for Vivado projects.

**Key Components:**
- `refClkNode`: Reference clock generation for Ethernet IP
- `ethNode` and `oxNode`: AXI-Lite and TileLink node instantiation
- `ethClient` and `oxClient`: Data bus connections from Rocket Core

### 2.2 OmniXtend Protocol Implementation

#### 2.2.1 OX.scala
Core OmniXtend node implementation providing TileLink manager functionality.

**Key Features:**
- `OmniXtendNode`: LazyModule implementing TileLink manager
- `OmniXtendBundle`: IO interface for Ethernet packet transmission
- Support for Get and PutFullData TileLink operations
- Direct integration with PacketTransceiver for packet conversion

**Platform-Specific Enhancements:**
- **VCU118-100G**: Includes PacketGenerator for performance testing
- **Arty100T**: Simplified implementation for resource-constrained FPGA

#### 2.2.2 PacketTransceiver.scala
Core packet conversion engine that transforms TileLink signals into Ethernet packets.

**Key Capabilities:**
- **TX Path**: TileLink to AXI-Stream conversion with Ethernet headers
- **RX Path**: AXI-Stream to TileLink conversion with response generation
- **Connection Management**: OmniXtend connection establishment and maintenance
- **Queue Management**: FIFO-based packet buffering and flow control

**Advanced Features:**
- Sequence number tracking and acknowledgment handling
- Credit-based flow control implementation
- Support for variable packet sizes (1-8 bytes)
- Comprehensive state machine for packet processing

#### 2.2.3 PacketGenerator.scala (VCU118-100G)
Advanced packet generation module for performance testing and validation.

**Testing Capabilities:**
- Throughput measurement with configurable packet counts
- Latency testing with precise timing measurement
- Real-time performance monitoring via VIO interface
- Automatic test pattern generation

#### 2.2.4 EndPoint.scala
Memory endpoint simulation providing read/write operations for testing.

**Memory Interface:**
- 1MB synchronous memory for data storage
- Address offset mapping (0x100000000)
- Queue-based serialization/deserialization
- Support for TileLink read and write operations

### 2.3 System Integration

#### 2.3.1 HarnessBinders.scala
Defines harness-level signal connections between ChipTop and TestHarness.

**Key Binders:**
- `WithEthernetAXI4Lite`: AXI-Lite bus connection for Ethernet control
- `WithOXTilelink`: TileLink bus connection for OmniXtend operations
- `WithREGTilelink`: Additional TileLink bus for register access (VCU118-100G)

#### 2.3.2 IOBinders.scala
Defines IO-level signal connections between DigitalTop and ChipTop.

**Key Binders:**
- `WithAXIIOPassthrough`: AXI-Lite IO passthrough for system integration
- `WithOXIOPassthrough`: TileLink IO passthrough for OmniXtend interface

#### 2.3.3 Configs.scala
Configuration management for memory regions and system parameters.

**Memory Regions:**
- `ExtBus`: Ethernet AXI-Lite (0x6000_0000–0x6001_0000)
- `ExtBus2`: Memory TileLink (0x2_0000_0000–0x2_1000_0000)
- `ExtBus3`: OX TileLink (0x1_0000_0000–0x2_0000_0000)

#### 2.3.4 Ports.scala
System port definitions with memory region parameterization.

#### 2.3.5 System.scala
Updated ChipyardSystem with OmniXtend support and TileLink master port capabilities.

### 2.4 Package Dependencies

#### 2.4.1 build.sbt
Dependency management for all project components.

**Key Dependencies:**
- `OXHost`: Core OmniXtend implementation
- `fpga_shells`: FPGA platform-specific shells
- `rocketchip`: Rocket Chip core components
- Platform-specific dependencies for each target

**Dependency Structure:**
```
chipyard
├── dependsOn(OXHost, fpga_shells, ...)
fpga_shells  
├── dependsOn(OXHost, rocketchip, ...)
OXHost
├── dependsOn(rocketchip, rocketchip_blocks)
```

## Usage

### Building for Different Platforms

#### Arty A7-100T
```bash
cd arty100t
make CONFIG=OXRocketConfig
```

#### VCU118 10G
```bash
cd VCU118-10G/chipyard
make CONFIG=OXRocketConfig
```

#### VCU118 100G
```bash
cd VCU118-100G/chipyard
make CONFIG=OXRocketConfig
```

### Testing and Validation

#### Throughput Testing
- Use PacketGenerator in test mode for automated throughput measurement
- Monitor performance via VIO interface in Vivado Hardware Manager
- Analyze packet statistics and timing information

#### Latency Testing
- Utilize LatencyGen module for precise latency measurement
- Measure round-trip time for TileLink operations
- Validate protocol compliance and performance

#### Memory Operations
- Use `devmem` commands to read/write memory via TileLink interface
- Verify data integrity across Ethernet transmission
- Test various packet sizes and transfer patterns

## Key Features

### Protocol Support
- **OmniXtend Protocol**: Full implementation of OmniXtend specification
- **TileLink over Ethernet**: Seamless TileLink to Ethernet packet conversion
- **Flow Control**: Credit-based flow control with sequence number tracking
- **Connection Management**: Automatic connection establishment and maintenance

### Performance Features
- **High Throughput**: Optimized for 10G and 100G Ethernet interfaces
- **Low Latency**: Efficient packet processing with minimal overhead
- **Scalable Architecture**: Modular design supporting multiple platforms
- **Real-time Monitoring**: Comprehensive debugging and performance monitoring

### Development Features
- **Modular Design**: Clean separation of concerns across components
- **Platform Abstraction**: Consistent interface across different FPGA targets
- **Extensible Architecture**: Easy addition of new platforms and features
- **Comprehensive Testing**: Built-in testing and validation capabilities

## Contributing

This project follows the Chipyard development model with modular components and clear interfaces. When contributing:

1. Follow the existing code structure and naming conventions
2. Ensure compatibility across all supported platforms
3. Add appropriate testing and validation for new features
4. Update documentation for any architectural changes

## License

This project is part of the Chipyard ecosystem and follows the same licensing terms as the main Chipyard repository.
