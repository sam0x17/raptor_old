#!/bin/bash
cd bin/data
num_rendered=$((`ls | grep -c '\.txt$'`))
echo $num_rendered training instances
cd ..
