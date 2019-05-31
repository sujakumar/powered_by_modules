#!/bin/bash

sdir="$(dirname $(readlink -f $0))"

echo "starting..."
nohup $sdir/extract.sh -l -m 100000 -s 3600 -d -1 -e CdkAdapter &
echo "kicked off."
