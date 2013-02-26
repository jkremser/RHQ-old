#!/bin/sh

#for use by startup scripts such as jboss which do not background themselves.
#see ServerControlPlugin

#set -x
exec "$@" &
PID=$!
wait $PID


#echo $PID
#echo "RHQ_CONTROL_WAIT=$RHQ_CONTROL_WAIT"

#if [ "x$RHQ_CONTROL_WAIT" = "x" ] ; then
#  echo "11111"
#  exit 0
#fi
#if [ "x$RHQ_CONTROL_WAIT" = "x0" ] ; then
#  echo "22222"
#  exit 0
#fi
#if [ "x$RHQ_CONTROL_WAIT" = "xtrue" ] ; then
#  echo "33333"
#  wait $PID
#else
  #sleep for a bit then check that the process is still alive,
  #otherwise the script failed right away perhaps due to syntax
  #error, invalid command line option, missing JAVA_HOME, etc.

#  echo "44444"
#  sleep $RHQ_CONTROL_WAIT
#  exit `kill -0 $PID 2>/dev/null`

  #if kill -0 $PID 2>/dev/null ; then
  #  exit 0
  #else
  #  exit 1
  #fi
#fi

