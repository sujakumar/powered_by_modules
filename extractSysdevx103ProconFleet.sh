#!/bin/bash

sdir="$(dirname $(readlink -f $0))"

export JAVA_OPTS=" -Xms4G -Xmx4G"
export GROOVY_OPTS=" -Xms4G -Xmx4G"

echo "starting..."
nohup $sdir/extract.sh -l -m 100000 -s 86400 -d -1 -e Sysdevx103ProconFleet &
echo "kicked off."

