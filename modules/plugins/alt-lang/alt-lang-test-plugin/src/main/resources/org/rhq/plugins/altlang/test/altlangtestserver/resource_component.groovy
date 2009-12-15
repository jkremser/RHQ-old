package org.rhq.plugins.altlang.test.altlangtestserver

import org.rhq.core.domain.measurement.AvailabilityType

File testDir = new File(System.getProperty("java.io.tmpdir"), "altlang")

if (action.name == 'start') {
  println "[Groovy] starting resource component"
  testDir.mkdir()
  new File(testDir, "${action.resourceType.name}.start").createNewFile()
}
else if (action.name == 'get_availability') {
  println "[Groovy] checking availability"
  return AvailabilityType.UP 
}
else if (action.name == 'stop') {
  println "[Groovy] stopping resource component"
  new File(testDir, "${action.resourceType.name}.stop").createNewFile()
}
