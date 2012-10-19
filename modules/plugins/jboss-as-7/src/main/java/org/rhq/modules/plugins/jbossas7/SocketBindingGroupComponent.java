package org.rhq.modules.plugins.jbossas7;

import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;

/**
 * Support for socket-binding-group. Especially we need to differentiate between domain
 * mode and standalone mode, which have different sets of properties
 * @author Heiko W. Rupp
 */
public class SocketBindingGroupComponent extends BaseComponent implements ConfigurationFacet{

//    private final Log log = LogFactory.getLog(SocketBindingGroupComponent.class);

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        Configuration config =  super.loadResourceConfiguration();

        if (!(context.getParentResourceComponent() instanceof StandaloneASComponent)) {
            config.put(new PropertySimple("port-offset",null));
        }

        return config;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {

        Configuration config = report.getConfiguration();
        if (!(context.getParentResourceComponent() instanceof StandaloneASComponent)) { // TODO what about managed servers
            config.put(new PropertySimple("port-offset",null));
        }

        super.updateResourceConfiguration(report);


    }
}
