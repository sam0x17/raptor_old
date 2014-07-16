#!/bin/bash
javac -cp .:imgscalr-lib-4.2.jar prepare_image.java prepare.java
#raptor_3dsfile="ships/hamina-class missile boat/Hamina.3DS" free_rot_vars=xyz start=0 end=0 blender -b -P gen_pose2.py
#exit;
CORES=$(grep -c ^processor /proc/cpuinfo 2>/dev/null || sysctl -n hw.ncpu)
start=1
end=500000
per=$((($end - $start) / $CORES))
echo $per
for (( i=0; i<CORES; i++ ))
do
	istart=$(($per * $i))
	iend=$(($istart + $per - 1))
	raptor_3dsfile="ships/hamina-class missile boat/Hamina.3DS" free_rot_vars=xyz start=$istart end=$iend blender -b -P gen_pose2.py &
done
istart=$(($per * $i))
iend=$end
raptor_3dsfile="ships/hamina-class missile boat/Hamina.3DS" free_rot_vars=xyz start=$istart end=$iend blender -b -P gen_pose2.py &
