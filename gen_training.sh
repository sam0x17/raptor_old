#!/bin/bash
./build.sh
cd bin
raptor_3dsfile="/home/sam/Desktop/raptor/old_src/ships/hamina-class missile boat/Hamina.3DS" free_rot_vars=xyz plan=B start=1 end=2 blender -b -P render.py
