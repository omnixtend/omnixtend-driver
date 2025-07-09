# OmniXtend Driver - Host Node Implementation

<div align="center">
  <img src="images/omnixtend-logo.png" alt="OmniXtend Logo" width="300">
</div>

A comprehensive implementation of OmniXtend protocol support for Chipyard-based systems, providing 10G and 100G Ethernet connectivity with TileLink over Ethernet (TLoE) capabilities.

## Overview

This project implements OmniXtend protocol support across multiple FPGA platforms, enabling high-performance communication between Rocket Chip-based systems and external endpoints via Ethernet. The implementation supports both 10G and 100G Ethernet interfaces with comprehensive packet processing capabilities, designed for FPGA and ASIC implementations.

## Features

- **OmniXtend Protocol Support**: Full implementation of OmniXtend specification
- **Multi-Platform Support**: Arty A7-100T, VCU118 10G, and VCU118 100G platforms
- **TileLink over Ethernet**: Seamless TileLink to Ethernet packet conversion
- **High Performance**: Optimized for 10G and 100G Ethernet interfaces
- **Flow Control**: Credit-based flow control with sequence number tracking
- **Connection Management**: Automatic connection establishment and maintenance
- **Real-time Monitoring**: Comprehensive debugging and performance monitoring
- **Modular Design**: Clean separation of concerns across components
- **Platform Abstraction**: Consistent interface across different FPGA targets

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

## Project Structure

```
├── arty100t/                       # Arty A7-100T platform implementation
├── VCU118-10G/                     # VCU118 10G platform implementation
├── VCU118-100G/                    # VCU118 100G platform implementation
├── images/                         # Project images and logos
└── LICENSE                         # License information
```

### Platform-Specific Structure

Each platform directory contains:

```
platform/
├── chipyard/                       # Chipyard project files
│   ├── src/main/scala/
│   │   ├── ethernet_subsystem/     # Ethernet IP integration
│   │   │   ├── xxv_ethernet.scala  # 10G Ethernet implementation
│   │   │   └── cmac_ethernet.scala # 100G Ethernet implementation
│   │   ├── UltraScaleShell.scala   # UltraScale+ shell implementation
│   │   ├── Ethernet.scala          # Ethernet overlay definitions
│   │   ├── TestHarness.scala       # Top-level test harness
│   │   ├── OX.scala                # Core OmniXtend node
│   │   ├── PacketTransceiver.scala # Packet conversion engine
│   │   ├── PacketGenerator.scala   # Performance testing module
│   │   ├── EndPoint.scala          # Memory endpoint simulation
│   │   ├── HarnessBinders.scala    # Harness-level connections
│   │   ├── IOBinders.scala         # IO-level connections
│   │   ├── Configs.scala           # Configuration management
│   │   ├── Ports.scala             # System port definitions
│   │   └── System.scala            # Updated ChipyardSystem
│   └── build.sbt                   # Build configuration
└── Makefile                        # Platform-specific build targets
```

## Architecture

### Rocket Core Integration
The **Rocket Core** serves as the primary processor and interfaces with external components through multiple bus interfaces:
- **SystemBus**: Main system interconnect
- **MemoryBus**: Memory access interface  
- **FrontBus**: Front-end peripheral interface
- **PeripheryBus**: Peripheral device interface

### Ethernet Subsystem
The **Ethernet Subsystem** handles high-speed data transmission and reception with external endpoints, comprising:
- **OmniXtend Module**: Core protocol implementation
- **Packet Generator**: Test packet generation for performance evaluation
- **Packet Transceiver**: TileLink to Ethernet packet conversion
- **Ethernet IP**: Xilinx Ethernet Subsystem IP integration

## Building

### Prerequisites

- Vivado Design Suite (2021.2 or later)
- Chipyard framework
- Scala 2.12 or later
- SBT (Scala Build Tool)

### Compilation

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

### Build Targets

- `CONFIG=OXRocketConfig`: Standard OmniXtend Rocket configuration
- `CONFIG=OXRocketConfig DEBUG=1`: Build with debug information
- `CONFIG=OXRocketConfig VERBOSE=1`: Build with verbose output

## Usage

### Hardware Generation

The implementation generates synthesizable Verilog code for each platform:

```scala
// Generate OmniXtend endpoint
val oxNode = LazyModule(new OmniXtendNode(params))
```

### Configuration Parameters

```scala
case class OmniXtendParams(
  dataWidth: Int = 64,
  addrWidth: Int = 32,
  maxOutstanding: Int = 8,
  windowSize: Int = 16,
  timeoutCycles: Int = 1000
)
```

### Interface Signals

The OmniXtend driver provides the following interfaces:

- **TileLink Interface**: Standard TileLink A/B/C/D/E channels
- **Ethernet Interface**: MAC layer interface for network communication
- **Control Interface**: Configuration and status signals
- **Memory Interface**: Direct memory access for high-performance operations

## Protocol Details

### Frame Structure

TLOE frames include:
- Header with sequence numbers, acknowledgments, and channel information
- Payload data
- Flow control credits

### Channel Types

- Channel 0: Control messages
- Channel A: TileLink requests
- Channel B: TileLink responses  
- Channel C: TileLink data
- Channel D: TileLink acknowledgments
- Channel E: Reserved

### Flow Control

Credit-based flow control prevents buffer overflow:
- Credits are exchanged during connection establishment
- Credits are consumed when sending messages
- Credits are replenished when receiving acknowledgments

## Testing and Validation

### Throughput Testing
- Use PacketGenerator in test mode for automated throughput measurement
- Monitor performance via VIO interface in Vivado Hardware Manager
- Analyze packet statistics and timing information

### Latency Testing
- Utilize LatencyGen module for precise latency measurement
- Measure round-trip time for TileLink operations
- Validate protocol compliance and performance

### Memory Operations
- Use `devmem` commands to read/write memory via TileLink interface
- Verify data integrity across Ethernet transmission
- Test various packet sizes and transfer patterns

## Performance Characteristics

### Throughput Performance
- **10G Platform**: Up to 10 Gbps sustained throughput
- **100G Platform**: Up to 100 Gbps sustained throughput
- **Arty Platform**: Up to 1 Gbps sustained throughput

### Latency Performance
- **Sub-microsecond**: Packet processing latency
- **Low overhead**: Minimal protocol overhead
- **Deterministic**: Predictable timing characteristics

### Reliability Features
- **Error detection**: Hardware-level error detection
- **Automatic retransmission**: Reliable packet delivery
- **Flow control**: Prevents buffer overflow

## Development

### Code Style

- Chisel 3.x coding standards
- Consistent naming conventions
- Comprehensive parameterization
- Modular design for reusability

### Testing

```bash
# Run unit tests
sbt test

# Run specific test
sbt "testOnly omnixtend.OmniXtendTest"

# Generate test waveforms
sbt "testOnly omnixtend.OmniXtendTest -- -DwriteVcd=1"
```

### Simulation

```bash
# Run simulation
sbt "runMain omnixtend.OmniXtendSimulator"

# Generate waveforms
sbt "runMain omnixtend.OmniXtendSimulator -- -DwriteVcd=1"
```

## Deployment

### FPGA Implementation

The generated Verilog can be synthesized for the following FPGA platforms:

- **Xilinx Artix-7**: Arty A7-100T development board
- **Xilinx Virtex UltraScale+**: VCU118 development board (10G and 100G variants)

### ASIC Implementation

The design is suitable for ASIC implementation with standard cell libraries.

## Contributing

This project follows the Chipyard development model with modular components and clear interfaces. When contributing:

1. Follow the existing code structure and naming conventions
2. Ensure compatibility across all supported platforms
3. Add appropriate testing and validation for new features
4. Update documentation for any architectural changes

## License

This project is part of the Chipyard ecosystem and follows the same licensing terms as the main Chipyard repository.

## Acknowledgments

- Based on the OmniXtend protocol specification
- Implements TileLink over Ethernet communication in hardware
- Designed for high-performance, reliable network communication
- Built with Chisel hardware construction language
- Integrated with Chipyard framework

---

<div align="center">
  <img src="images/ETRI_CI_01.png" alt="ETRI Logo" width="200">
  <br><br>
  <strong>This project is developed and maintained by <a href="https://www.etri.re.kr/">ETRI (Electronics and Telecommunications Research Institute)</a></strong>
</div>
