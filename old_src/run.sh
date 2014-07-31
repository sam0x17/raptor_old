#!/bin/bash
javac -cp .:imgscalr-lib-4.2.jar prepare_image.java
raptor_3dsfile="ships/hamina-class missile boat/Hamina.3DS" free_rot_vars=xyz blender -b -P gen_pose.py
