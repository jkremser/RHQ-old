function usage() {
        println("Usage: deploy groupName");
        throw "Illegal arguments";
}

if( args.length < 1 ) usage();
var groupName = args[0];

var rg = new ResourceGroup(resType);
rg.setRecursive(false);
rg.setDescription("Created via groupcontrol scripts on " + new java.util.Date().toString());
rg.setName(groupName);

rg = ResourceGroupManager.createResourceGroup(rg);

var resType = ResourceTypeManager.getResourceTypeByNameAndPlugin("JBossAS 5 Server","JBossAS5");
