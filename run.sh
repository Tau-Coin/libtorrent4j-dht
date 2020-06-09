#!/bin/bash
currDir=$(pwd)
echo $currDir

classpath=$currDir/libs/libtorrent4j.jar:$currDir/build/classes/java/main
echo $classpath

libpath="java.library.path=$currDir/libs"
echo $libpath

mainclass=io.tau.DhtShell

java -cp $classpath -D$libpath $mainclass
