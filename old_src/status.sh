#!/bin/bash
cd DATA
num_rendered=$((`ls -l | wc -l` / 2))
echo $num_rendered
cd ..
