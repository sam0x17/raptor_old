#!/bin/bash
./build.sh
cd bin
CORES=$(grep -c ^processor /proc/cpuinfo 2>/dev/null || sysctl -n hw.ncpu)
start=$1
end=$2
per=$((($end - $start) / $CORES))
echo $per
for (( i=0; i<$CORES; i++ ))
do
	istart=$(($per * $i))
	iend=$(($istart + $per - 1))
	raptor_3dsfile="ships/Hamina.3DS" free_rot_vars=xyz plan=B start=$istart end=$iend blender -b -P render.py &
done
istart=$(($per * $i))
iend=$end
raptor_3dsfile="ships/Hamina.3DS" free_rot_vars=xyz plan=B start=$istart end=$iend blender -b -P render.py &
