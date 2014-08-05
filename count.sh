#!/bin/bash
cd bin/data
num_rendered=$((`ls *.txt -l | wc -l`))
num_crop=$((`ls *crop* -l | wc -l`))
echo $num_rendered training instances
echo $num_crop OK
cd ..
