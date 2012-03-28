function usage() {
        println("Usage: metrics groupName metricName");
        throw "Illegal arguments";
}

if( args.length < 2 ) usage();
var groupName = args[0];
var metricName = args[1];

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

//var rt = ResourceTypeManager.getResourceTypeByNameAndPlugin("JBossAS 5 Server","JBossAS5");
var rt = ResourceTypeManager.getResourceTypeByNameAndPlugin("JBossAS Server","JBossAS");
var mdc = MeasurementDefinitionCriteria();
mdc.addFilterDisplayName(metricName);
mdc.addFilterResourceTypeId(rt.id);
var mdefs =  MeasurementDefinitionManager.findMeasurementDefinitionsByCriteria(mdc);
var resources = ResourceManager.findResourcesByCriteria(criteria);
var metrics = MeasurementDataManager.findLiveData(resources.get(0).id, [mdefs.get(0).id]);

if( metrics !=null ) {
        println(" Metric value for " + resources.get(0).id + " is " + metrics );
}


