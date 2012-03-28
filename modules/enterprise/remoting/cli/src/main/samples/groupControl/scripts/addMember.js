function usage() {
        println("Usage: addMember groupName resourceName resourceTypeName");
        throw "Illegal arguments";
}

if( args.length < 3 ) usage();
var groupName = args[0];
var resourceName = args[1];
var resourceTypeName = args[2];

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
criteria.addFilterName(resourceName);
criteria.addFilterResourceTypeName(resourceTypeName);

var resources = ResourceManager.findResourcesByCriteria(criteria);
if( resources != null ) {
  if( resources.size() > 1 ) {
        println("Found more than one JBossAS Server instance. Try to specialize.");
     for( i =0; i < resources.size(); ++i) {
          var resource = resources.get(i);
          println("  found " + resource.name );
     }
  }
  else if( resources.size() == 1 ) {
     resource = resources.get(0);
     println("Found one JBossAS Server instance. Trying to add it.");
     println("  " + resource.name );
        ResourceGroupManager.addResourcesToGroup(group.id, [resource.id]);
     println("  Added to " + group.name + "!");
  }
  else {
        println("Did not find any JBossAS Server instance matching your pattern. Try again.");
  }
}
