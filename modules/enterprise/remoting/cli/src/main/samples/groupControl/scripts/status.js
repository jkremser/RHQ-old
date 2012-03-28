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
     println("  found " + resource.name );
}

var server = ProxyFactory.getResource(resource.id);
var avail  = AvailabilityManager.getCurrentAvailabilityForResource(server.id);

println("  " + server.name );
println("    - Availability: " + avail.availabilityType.getName());
println("    - Started     : " + avail.startTime.toGMTString());
println("");

var avail = AvailabilityManager.getCurrentAvailabilityForResource(server.id);

if( avail.availabilityType.toString() == "DOWN" ) {
           println("  Server is DOWN. Please first start the server and run this script again!");
           println("");
}
