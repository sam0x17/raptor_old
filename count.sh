#!/bin/bash
cd bin/data
num_rendered=$((`ls -l | wc -l` / 2))
echo $num_rendered training instances
cd ..
