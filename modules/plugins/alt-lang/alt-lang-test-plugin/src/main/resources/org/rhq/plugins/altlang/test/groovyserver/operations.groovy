package org.rhq.plugins.altlang.test.groovyserver

import org.rhq.core.pluginapi.operation.OperationResult

if (action.name == 'echo') {
  return echo()  
}

def echo() {
  def msg = parameters.getSimple('msg').stringValue
  return new OperationResult(msg)
}


