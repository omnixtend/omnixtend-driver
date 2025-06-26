package chipyard

import freechips.rocketchip.config.{Config}
import freechips.rocketchip.diplomacy.{AsynchronousCrossing}

// --------------
// OmniXtend Rocket Configs
// --------------

class OXRocketConfig extends Config(
//  new chipyard.config.WithL2TLBs(1024) ++
  new omnixtend.WithOX(useAXI4=false, useBlackBox=false) ++ 
//  new freechips.rocketchip.subsystem.WithInclusiveCache(nWays=4, capacityKB=2048) ++
  new freechips.rocketchip.subsystem.WithNBigCores(1) ++         // single rocket-core
  new chipyard.config.AbstractConfig)

