#!/bin/bash
cd bin/data
num_rendered=$((`ls *.txt -l | wc -l`))
echo $num_rendered training instances
cd ..
