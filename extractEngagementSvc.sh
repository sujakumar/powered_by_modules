#!/bin/bash

sdir="$(dirname $(readlink -f $0))"

echo "starting..."
nohup $sdir/extract.sh -l -m 100000 -s 360 -d -1 -e EngagementSvc &
echo "kicked off."

