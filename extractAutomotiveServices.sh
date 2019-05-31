#!/bin/bash

sdir="$(dirname $(readlink -f $0))"

echo "starting..."
nohup $sdir/extract.sh -l -m 100000 -s 360 -d 3 -e AutomotiveServices &
echo "kicked off."

