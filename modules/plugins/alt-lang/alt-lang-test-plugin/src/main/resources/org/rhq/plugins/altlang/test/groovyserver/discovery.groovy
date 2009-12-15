package org.rhq.plugins.altlang.test.groovyserver

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails
import org.rhq.core.domain.resource.ResourceType

println "Executing groovy discovery"

details = new HashSet()
details << new DiscoveredResourceDetails(discoveryContext.resourceType,
                                          "1",
                                          "GroovyServer",
                                          "1.0",
                                          "Alt Lang Test Server",
                                          discoveryContext.defaultPluginConfiguration,
                                          null)

return details
