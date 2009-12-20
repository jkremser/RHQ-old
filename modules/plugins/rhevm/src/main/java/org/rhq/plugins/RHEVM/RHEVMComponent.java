
package org.rhq.plugins.RHEVM;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.plugin.PluginContext;
import org.rhq.core.pluginapi.plugin.PluginLifecycleListener;
import org.rhq.core.pluginapi.support.SnapshotReportRequest;
import org.rhq.core.pluginapi.support.SnapshotReportResults;
import org.rhq.core.pluginapi.support.SupportFacet;


public class RHEVMComponent implements ResourceComponent
, MeasurementFacet
, OperationFacet
, ConfigurationFacet
, CreateChildResourceFacet
, DeleteResourceFacet
, PluginLifecycleListener
, SupportFacet
{
    private final Log log = LogFactory.getLog(this.getClass());

    private static final int CHANGEME = 1; // TODO remove or change this


    public static final String DUMMY_EVENT = "RHEVMDummyEvent"; // Same as in Plugin-Descriptor

    EventContext eventContext;

    /**
     * Callback when the plugin is created
     * @see org.rhq.core.pluginapi.plugin.PluginLifecycleListener#initialize(PluginContext)
     */
    public void initialize(PluginContext context) throws Exception
    {
    }

    /**
     * Callback when the plugin is unloaded
     * @see org.rhq.core.pluginapi.plugin.PluginLifecycleListener#shutdown()
     */
    public void shutdown()
    {
    }

    /**
     * Return availability of this resource
     *  @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() {
        // TODO supply real implementation
        return AvailabilityType.UP;
    }


    /**
     * Start the resource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {

        Configuration conf = context.getPluginConfiguration();
        // TODO add code to start the resource / connection to it

        eventContext = context.getEventContext();
        RHEVMEventPoller eventPoller = new RHEVMEventPoller();
        eventContext.registerEventPoller(eventPoller, 60);

    }


    /**
     * Tear down the rescource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#stop()
     */
    public void stop() {


        eventContext.unregisterEventPoller(DUMMY_EVENT);
    }



    /**
     * Gather measurement data
     *  @see org.rhq.core.pluginapi.measurement.MeasurementFacet#getValues(org.rhq.core.domain.measurement.MeasurementReport, java.util.Set)
     */
    public  void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

         for (MeasurementScheduleRequest req : metrics) {
            if (req.getName().equals("dummyMetric")) {
                 MeasurementDataNumeric res = new MeasurementDataNumeric(req, Double.valueOf(CHANGEME));
                 report.addData(res);
            }
            // TODO add more metrics here
         }
    }



    public void startOperationFacet(OperationContext context) {

    }


    /**
     * Invokes the passed operation on the managed resource
     * @param name Name of the operation
     * @param params The method parameters
     * @return An operation result
     * @see org.rhq.core.pluginapi.operation.OperationFacet
     */
    public OperationResult invokeOperation(String name, Configuration params) throws Exception {

        OperationResult res = new OperationResult();
        if ("dummyOperation".equals(name)) {
            // TODO implement me

        }
        return res;
    }


    /**
     * Load the configuration from a resource into the configuration
     * @return The configuration of the resource
     * @see org.rhq.core.pluginapi.configuration.ConfigurationFacet
     */
    public Configuration loadResourceConfiguration()
    {
        // TODO supply code to load the configuration from the resource into the plugin
        return null;
    }

    /**
     * Write down the passed configuration into the resource
     * @param report The configuration updated by the server
     * @see org.rhq.core.pluginapi.configuration.ConfigurationFacet
     */
    public void updateResourceConfiguration(ConfigurationUpdateReport report)
    {
        // TODO supply code to update the passed report into the resource
    }

    /**
     * Create a child resource
     * @see org.rhq.core.pluginapi.inventory.CreateChildResourceFacet
     */
    public CreateResourceReport createResource(CreateResourceReport report)
    {
        // TODO supply code to create a child resource

        return null; // TODO change this
    }

    /**
     * Delete a child resource
     * @see org.rhq.core.pluginapi.inventory.DeleteResourceFacet
     */
    public void deleteResource() throws Exception
    {
        // TODO supply code to delete a child resource
    }

    /**
     * Takes a snapshot and returns the snapshot report content in the given stream. A facet implementation
     * can support different kinds of snapshots, the given name determines which kind of snapshot to take.
     *
     * @param request identifies the type of snapshot to take
     * @return snapshot results, including a stream containing the contents of the snapshot report
     * @throws Exception if failed to generate the snapshot report
     */
    public SnapshotReportResults getSnapshotReport(SnapshotReportRequest request) throws Exception
    {
        // TODO
        return null;
    }
}
