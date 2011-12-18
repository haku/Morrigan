#!/bin/sh
cd `dirname $0`
if [ -f "morrigan" ] ; then
  ./morrigan
elif [ -f "morriganserver" ] ; then
  ./morriganserver
fi
