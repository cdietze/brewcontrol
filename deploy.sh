#!/bin/sh
set -e -x

scp ./target/brewcontrol-2.0-SNAPSHOT.jar pi:~/brewcontrol
ssh pi sudo cp -v /home/pi/brewcontrol/brewcontrol-2.0-SNAPSHOT.jar /usr/local/lib/
rsync -avz ./root/ pi:~/brewcontrol/root/
ssh pi sudo cp -rv /home/pi/brewcontrol/root/* /
ssh pi sudo /etc/init.d/brewcontrol restart