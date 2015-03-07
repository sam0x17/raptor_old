#!/bin/bash
mkdir bin -p
mkdir bin/data -p
mv bin/data .
rm bin/* -r -f
mv data bin
cp src/ships bin -r
mkdir bin/linear_experiment
jars=".:bin"
for f in lib/*.jar
do
	jars="$jars:$f"
done
javas=`find src -name *.java`
pythons=`find src -name *.py`
javac -cp $jars -s src -d bin $javas || exit 1
cp $pythons bin || exit 1
