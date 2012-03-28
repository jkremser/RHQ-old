#!/bin/bash
#
# groupcontrol
# ------------
# This is a simple wrapper script for all the java script scripts in this folder.
# Start this script with some parameters to automate group handling from within the
# command line.
# 
# With groupcontrol you can do the following:
#   create   : Create a new group
#   addMember: Add a new EAP instance to the specified group
#   status   : Print the status of all resources of a group
#   start    : start all EAP instances specified by group name
#   deploy   : Deploys an application to all AS instances specified by group name
#   ops      : Runs an operation on all AS instances specified by group name
#   metrics  : Gets the specified metric value for all AS instances specified by group name
#
# 
			
## Should not be run as root.
if [ "$EUID" = "0" ]; then
   echo " Please use a normal user account and not the root account"
   exit 1
fi
		
## Figure out script home
MY_HOME=$(cd `dirname $0` && pwd)
SCRIPT_HOME=$MY_HOME/scripts
			
## Source some defaults
. $MY_HOME/groupcontrol.conf
			
## Check to see if we have a valid CLI home
if [ ! -d ${JON_CLI_HOME} ]; then
     echo "JON_CLI_HOME not correctly set. Please do so in the file"
     echo $MY_HOME/groupcontrol.conf
     exit 1
fi
			
RHQ_OPTS="-s $JON_HOST -u $JON_USER -t $JON_PORT"
# If JBoss ON_PWD is given then use it as argument. Else let the user enter the password
if [ "x$JON_PWD" == "x" ]; then
     RHQ_OPTS="$RHQ_OPTS -P"
else
     RHQ_OPTS="$RHQ_OPTS -p $JON_PWD"
fi
			
#echo "Calling groupcontrol with $RHQ_OPTS"
			
usage() {
     echo "  Usage $0:"
     echo "  Use this tool to control most group related tasks with a simple script."
     echo "  ------------------------------------------------------------------------- "
}

doDeploy() {
     $JON_CLI_HOME/bin/rhq-cli.sh $RHQ_OPTS -f $SCRIPT_HOME/deploy.js $2 $3
}

doCreate() {
     $JON_CLI_HOME/bin/rhq-cli.sh $RHQ_OPTS -f $SCRIPT_HOME/group.js $2
}
					
doAddMember() {
     $JON_CLI_HOME/bin/rhq-cli.sh $RHQ_OPTS -f $SCRIPT_HOME/addMember.js $2 $3 $4
}	
			
doStatus() {
     $JON_CLI_HOME/bin/rhq-cli.sh $RHQ_OPTS -f $SCRIPT_HOME/status.js $2
}

doRestart() {
     $JON_CLI_HOME/bin/rhq-cli.sh $RHQ_OPTS -f $SCRIPT_HOME/restart.js $2 
}

doAvail() {
     $JON_CLI_HOME/bin/rhq-cli.sh $RHQ_OPTS -f $SCRIPT_HOME/avail.js
}	
		
doMetrics() {
     $JON_CLI_HOME/bin/rhq-cli.sh $RHQ_OPTS -f $SCRIPT_HOME/metrics.js $2 $3
}

case "$1" in
'deploy')
	doDeploy $*
	;;     
'create')
	doCreate $*
	;;     
'addMember')
	doAddMember $*
	;;     
'status')
	doStatus $*
	;;     
'restart')
	doRestart $*
	;;     
'avail')
	doAvail $*
	;;     
'metrics')
	doMetrics $*
	;;     
*)
        usage $*
        ;;
esac
