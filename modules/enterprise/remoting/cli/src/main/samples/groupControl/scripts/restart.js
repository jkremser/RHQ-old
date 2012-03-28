function usage() {
        println("Usage: status groupName");
        throw "Illegal arguments";
}

if( args.length < 1 ) usage();
var groupName = args[0];

groupcriteria = new ResourceGroupCriteria();
groupcriteria.addFilterName(groupName);

var groups = ResourceGroupManager.findResourceGroupsByCriteria(groupcriteria);
if( groups != null ) {
  if( groups.size() > 1 ) {
        println("Found more than one group.");
  }
  else if( groups.size() == 1 ) {
     group = groups.get(0);
  }
}

criteria = new ResourceCriteria();
criteria.addFilterExplicitGroupIds(group.id);

var resources = ResourceManager.findResourcesByCriteria(criteria);
for( i =0; i < resources.size(); ++i) {
     var resource = resources.get(i);
     var resType = resource.resourceType.name;
     println("  found " + resource.name );

     if( resType != "JBossAS Server") {
          println("    ---> Resource not of required type. Exiting!");
          usage();
     }

     var server = ProxyFactory.getResource(resource.id);
     println("    stopping " + server.name + "....");
     try {
         server.shutdown()
     }
     catch( ex ) {
         println("   --> Caught " + ex );
     }
				
     println("    restarting " + server.name + "....." );
     try {
         server.start();
     }
     catch( ex ) {
         println("   --> Caught " + ex );
     }
}
