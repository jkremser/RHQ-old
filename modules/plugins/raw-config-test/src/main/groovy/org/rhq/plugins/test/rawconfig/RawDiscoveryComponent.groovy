package org.rhq.plugins.test.rawconfig

import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext

class RawDiscoveryComponent implements ResourceDiscoveryComponent {

  Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
    def details = new HashSet()

    details << new DiscoveredResourceDetails(context.resourceType,
                                             "1",
                                             "Raw Server",
                                             "1.0",
                                             "A fake server component for testing raw config",
                                             context.defaultPluginConfiguration,
                                             null)
  }


}
