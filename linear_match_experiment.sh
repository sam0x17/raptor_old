#!/bin/bash
./build.sh
cd bin
java -cp .:lib:bin:../lib/imgscalr-lib-4.2.jar:../lib/jblas-1.2.3.jar raptor.LinearMatchExperiment data
