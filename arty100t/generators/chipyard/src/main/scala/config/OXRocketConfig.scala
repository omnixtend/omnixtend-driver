package chipyard

import org.chipsalliance.cde.config.Config
import freechips.rocketchip.prci.AsynchronousCrossing
import freechips.rocketchip.rocket.WithNBigCores
import freechips.rocketchip.subsystem._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
// Import the OX-related configs - adjust path as needed
// Import OX classes
import omnixtend._

// --------------
// OmniXtend Rocket Configs
// --------------

class OXRocketConfig extends Config(
  new omnixtend.WithOX(useAXI4=false, useBlackBox=false) ++ 
  new WithNBigCores(1) ++             // single rocket-core
  new chipyard.config.AbstractConfig)

