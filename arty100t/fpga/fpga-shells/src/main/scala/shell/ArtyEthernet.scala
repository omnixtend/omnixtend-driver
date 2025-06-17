package sifive.fpgashells.shell

import chisel3._
import freechips.rocketchip.diplomacy._
import org.chipsalliance.cde.config._

import freechips.rocketchip.tilelink._
import sifive.fpgashells.clocks._
import freechips.rocketchip.prci._

/**
*

**/

case class ArtyEthernetShellInput()
/**
 * Design input parameters for the Arty Ethernet interface
 * @param baseAddress Base address for the Ethernet controller (default: 0x60000000L)
 * @param clockWrangler Clock adapter node for managing clock domains
 * @param corePLL PLL node for core clock generation
 */
case class ArtyEthernetDesignInput(
	baseAddress: BigInt = 0x60000000L,
	clockWrangler: ClockAdapterNode,
	corePLL: PLLNode
)(implicit val p: Parameters)

case class ArtyEthernetOverlayOutput(eth: TLInwardNode, oxnode: TLInwardNode )

case object ArtyEthernetOverlayKey extends Field[Seq[DesignPlacer[ArtyEthernetDesignInput, ArtyEthernetShellInput, ArtyEthernetOverlayOutput]]](Nil)
trait ArtyEthernetShellPlacer[Shell] extends ShellPlacer[ArtyEthernetDesignInput, ArtyEthernetShellInput, ArtyEthernetOverlayOutput]

abstract class ArtyEthernetPlacedOverlay[IO <: Data](
  val name: String, val di: ArtyEthernetDesignInput, val si: ArtyEthernetShellInput)
    extends IOPlacedOverlay[IO, ArtyEthernetDesignInput, ArtyEthernetShellInput, ArtyEthernetOverlayOutput]
{
  implicit val p = di.p
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
