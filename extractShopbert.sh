#!/bin/bash

sdir="$(dirname $(readlink -f $0))"

export JAVA_OPTS=" -Xms4G -Xmx4G"
export GROOVY_OPTS=" -Xms4G -Xmx4G"

echo "starting..."
nohup $sdir/extract.sh -l -m 100000 -s 3600 -d 3 -e Shopbert &
echo "kicked off."



