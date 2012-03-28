function usage() {
	println("Usage: deployToGroup <fileName> <groupName>");
	throw "Illegal arguments";
}

function PackageParser(fullPathName) {
	var file = new java.io.File(fullPathName);
		
	var fileName = file.getName();
	var packageType = fileName.substring(fileName.lastIndexOf('.')+1);
	var tmp = fileName.substring(0, fileName.lastIndexOf('.'));
	var realName = tmp.substring(0, tmp.lastIndexOf('-'));
	var version = tmp.substring(tmp.lastIndexOf('-') + 1);			
	var packageName = realName + "." + packageType;
	
	this.packageType = packageType.toLowerCase();
	this.packageName = packageName;
	this.version     = version;
	this.realName    = realName;
}

if( args.length < 2 ) usage();
				
var fileName = args[0];
var groupName = args[1];

var file = new java.io.File(fileName);
				
if( !file.exists() ) {
    println(fileName + " does not exist!");
    usage();
}
				
if( !file.canRead() ) {
    println(fileName + " can't be read!");
    usage();
}

var rgc = new ResourceGroupCriteria();
rgc.addFilterName(groupName);
rgc.fetchExplicitResources(true);
var groupList = ResourceGroupManager.findResourceGroupsByCriteria(rgc);

rgc.fetchExplicitResources(true);

if( groupList == null || groupList.size() != 1 ) {
    println("Can't find a resource group named " + groupName);
    usage();
}
				
var group = groupList.get(0);
				
println("  Found group: " + group.name );
println("  Group ID   : " + group.id );
println("  Description: " + group.description);

if( group.explicitResources == null || group.explicitResources.size() == 0 ) {
    println("  Group does not contain explicit resources --> exiting!" );
    usage();
}
var resourcesArray = group.explicitResources.toArray();

for( i in resourcesArray ) {
    var res = resourcesArray[i];
    var resType = res.resourceType.name;
    println("  Found resource " + res.name + " of type " + resType + " and ID " + res.id);
				
    if( resType != "JBossAS5 Server") {
        println("    ---> Resource not of required type. Exiting!");
        usage();
    }
			
    var server = ProxyFactory.getResource(res.id);
}

var children = server.children;
for( c in children ) {
    var child = children[c];
				
    if( child.name == packageName ) {
    }
}

var avail = AvailabilityManager.getCurrentAvailabilityForResource(server.id);

if( avail.availabilityType.toString() == "DOWN" ) {
	   println("  Server is DOWN. Please first start the server and run this script again!");
	   println("");
	   continue;
}
				
println("    uploading new application code");
child.updateBackingContent(fileName);

var appType = ResourceTypeManager.getResourceTypeByNameAndPlugin( appTypeName, "JBossAS5" );
if( appType == null ) {
    println("  Could not find application type. Exit.");
    usage();
}

var realPackageType = ContentManager.findPackageTypes( appTypeName, "JBossAS5" );
				
if( realPackageType == null ) {
    println("  Could not find JBoss ON's packageType. Exit.");
    usage();
}

var deployConfig = new Configuration();
deployConfig.put( new PropertySimple("deployExploded", "false"));
deployConfig.put( new PropertySimple("deployFarmed", "false"));

var deployConfigDef = ConfigurationManager.getPackageTypeConfigurationDefinition(realPackageType.getId());

var inputStream = new java.io.FileInputStream(file);
var fileLength = file.length();
var fileBytes = java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, fileLength);
for (numRead=0, offset=0; ((numRead >= 0) && (offset < fileBytes.length)); offset += numRead ) {
    numRead = inputStream.read(fileBytes, offset, fileBytes.length - offset); 	
}

ResourceFactoryManager.createPackageBackedResource(
    server.id,
    appType.id,
    packageName,
    null,  // pluginConfiguration
    packageName,
    packageVersion,
    null, // architectureId        
    deployConfig,
    fileBytes,
    null // timeout
);

				
