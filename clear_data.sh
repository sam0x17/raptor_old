#!/bin/bash
read -p "Clear out data and logs? (y/n)?" choice
case "$choice" in 
  y|Y )
    echo "clearing data..."
    rm bin/data -r -f
    echo "clearing logs..."
    rm bin/*.log
    echo "done.";;
  n|N ) exit 0;;
  * ) echo "invalid";;
esac
