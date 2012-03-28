println("Scanning all RHQ Agent instances");
var rc = ResourceCriteria();
var resType = ResourceTypeManager.getResourceTypeByNameAndPlugin("RHQ Agent", "RHQAgent");
rc.addFilterPluginName("RHQAgent");
rc.addFilterResourceTypeName("RHQ Agent");
rc.addFilterParentResourceTypeId("10001");

var resources = ResourceManager.findResourcesByCriteria(rc).toArray();

var idx=0;
for( i in resources ) {
     if( resources[i].resourceType.id == resType.id ) {
          resources[idx] = resources[i];
          idx = idx + 1;
     }
}

for( a in resources ) {
     var agent = resources[a]

     var resType = agent.resourceType.name;
     println("  Found resource " + agent.name + " of type " + resType + " and ID " + agent.id);

     println("  executing availability scan on agent" );
     println("    -> " + agent.name + " / " + agent.id);
     var config = new Configuration();
     config.put(new PropertySimple("changesOnly", "true") );
     var ros = OperationManager.scheduleResourceOperation(
          agent.id,
          "executeAvailabilityScan",
          0,
          1,
          0,
          10000000,
          config,
          "test from cli"
     );

     println(ros);
     println("");
}

