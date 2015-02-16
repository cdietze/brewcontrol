#!/bin/sh
set -e
sbt warn assembly
scp target/scala-2.11/brewcontrol-assembly-0.1-SNAPSHOT.jar pi:~/brewcontrol
ssh pi sudo java -jar ./brewcontrol/brewcontrol-assembly-0.1-SNAPSHOT.jar