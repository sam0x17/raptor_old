#!/bin/bash
mkdir bin -p
rm bin -r -f || exit 1
jars=".:bin"
for f in lib/*.jar
do
	jars="$jars:$f"
done
javas=`find src -name *.java`
pythons=`find src -name *.py`
javac -cp $jars -s src -d bin $javas || exit 1
cp $pythons bin || exit 1
