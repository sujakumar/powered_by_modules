#!/bin/bash

sdir="$(dirname $(readlink -f $0))"

echo "starting..."
nohup $sdir/extract.sh -l -m 100000 -s 21600 -d -1 -e FullProconCore &
echo "kicked off."

