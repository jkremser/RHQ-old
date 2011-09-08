package org.rhq.modules.plugins.jbossas7;

import java.util.HashMap;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Handle JDBC-driver related stuff
 * @author Heiko W. Rupp
 */
public class DatasourceComponent extends BaseComponent implements OperationFacet {

    private static final String NOTSET = "-notset-";

    @Override
    public OperationResult invokeOperation(String operationName,
                                           Configuration parameters) throws Exception {

        OperationResult result = new OperationResult();
        ASConnection connection = getASConnection();
        Operation op;

        if (operationName.equals("addDriver")) { // TODO decide if we need this at all. See also the plugin-descriptor
            String drivername = parameters.getSimpleValue("driver-name", NOTSET);

            Address theAddress = new Address(address);
            theAddress.add("jdbc-driver", drivername);

            op =  new Operation("add",theAddress);
            op.addAdditionalProperty("driver-name",drivername);
            op.addAdditionalProperty("deployment-name",parameters.getSimpleValue("deployment-name", NOTSET));
            op.addAdditionalProperty("driver-class-name",parameters.getSimpleValue("driver-class-name", NOTSET));
        }
        else if (operationName.equals("addDatasource")) {
            String name = parameters.getSimpleValue("name",NOTSET);

            Address theAddress = new Address(address);
            theAddress.add("data-source", name);
            op = new Operation("add",theAddress);
            addRequiredToOp(op,parameters,"driver-name");
            addRequiredToOp(op,parameters,"jndi-name");
            addRequiredToOp(op, parameters, "pool-name");
            addRequiredToOp(op, parameters, "connection-url");
            addOptionalToOp(op, parameters, "user-name");
            addOptionalToOp(op,parameters,"password");
        }
        else if (operationName.equals("addXADatasource")) {
            String name = parameters.getSimpleValue("name",NOTSET);

            Address theAddress = new Address(address);
            theAddress.add("xa-data-source",name);
            op = new Operation("add",theAddress);
            addRequiredToOp(op,parameters,"driver-name");
            addRequiredToOp(op,parameters,"jndi-name");
            addRequiredToOp(op,parameters,"pool-name");
            addRequiredToOp(op,parameters,"connection-url");
            addOptionalToOp(op,parameters,"user-name");
            addOptionalToOp(op,parameters,"password");
            addRequiredToOp(op,parameters,"xa-data-source-class");

            Map<String,Object> props = new HashMap<String, Object>(); // TODO
            props.put("_foo","_bar"); // TODO AS7-1209
            op.addAdditionalProperty("xa-data-source-properties",props);
        }
        else {
            /*
             * This is a catch all for operations that are not explicitly treated above.
             */
            op = new Operation(operationName,address);
        }

        Result res = connection.execute(op);
        if (res.isSuccess()) {
            result.setSimpleResult("Success");
        }
        else {
            result.setErrorMessage(res.getFailureDescription());
        }
        
        return result;
    }

    private void addAdditionalToOp(Operation op, Configuration parameters, String property, boolean optional) {
        PropertySimple ps = parameters.getSimple(property);
        if (ps==null) {
            if (!optional)
                throw new IllegalArgumentException("Property " + property + " not found for required parameter");
        }
        else {
            String tmp = ps.getStringValue();
            if (tmp!=null) {
                op.addAdditionalProperty(property,tmp);
            }
        }
    }

    private void addRequiredToOp(Operation op, Configuration parameters, String property)  {
        addAdditionalToOp(op,parameters,property,false);
    }

    private void addOptionalToOp(Operation op, Configuration parameters, String property) {
        addAdditionalToOp(op,parameters,property,true);
    }
}
