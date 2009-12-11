package org.rhq.plugins.altlang.test.altlangtestserver

import org.rhq.core.domain.measurement.AvailabilityType

File testDir = new File(System.getProperty("java.io.tmpdir"), "altlang")

if (action.name == 'start') {
  println "[Groovy] starting resource component"
  testDir.mkdir()
  new File(testDir, "${action.resourceType.name}.start").createNewFile()
}

if (action.name == 'get_availability') {
  println "[Groovy] checking availability"
  return AvailabilityType.UP 
}
