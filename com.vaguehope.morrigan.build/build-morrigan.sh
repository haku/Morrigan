#!/bin/sh

BASEDIR=$(dirname $0)
cd $BASEDIR
BASEDIR=`pwd`

SHELFDIR=$BASEDIR/shelf
TABLEDIR=$BASEDIR/table

if [ -e $TABLEDIR ] ; then
  rm -rf $TABLEDIR/*
  rmdir $TABLEDIR
  if [ $? -ne 0 ] ; then
    echo "Failed to delete table directory $TABLEDIR." >&2
    exit 2
  fi
fi
mkdir $TABLEDIR

tar xf $SHELFDIR/eclipse-jee-helios-SR1-linux-gtk.tar.gz -C $TABLEDIR
mv $TABLEDIR/eclipse $TABLEDIR/eclipse-jee-helios
unzip -q $SHELFDIR/eclipse-3.6.1-delta-pack.zip -d $TABLEDIR
mv $TABLEDIR/eclipse $TABLEDIR/eclipse-3.6.1-delta-pack

cd $TABLEDIR
mkdir src
cd src
svn co https://aomushi:43443/svn/morrigan/trunk/
cd ../..

$TABLEDIR/eclipse-jee-helios/eclipse \
 -noSplash \
 -application org.eclipse.ant.core.antRunner \
 -data $TABLEDIR/src/trunk \
 -buildfile $TABLEDIR/src/trunk/com.vaguehope.morrigan.build/build-in-eclipse.xml

#mkdir output
#cp $TABLEDIR/src/trunk/com.vaguehope.morrigan.build/builds/* output/

