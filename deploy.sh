#!/bin/sh
set -e -x
sbt warn assembly
scp target/scala-2.11/brewcontrol-assembly-0.1-SNAPSHOT.jar pi:~/brewcontrol
rsync -avz ./root/ pi:~/brewcontrol/root/
ssh pi sudo cp -rv /home/pi/brewcontrol/root/* /
ssh pi sudo /etc/init.d/brewcontrol restart