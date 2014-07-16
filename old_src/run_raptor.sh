#!/bin/bash
javac -cp .:encog-core-3.2.0.jar:imgscalr-lib-4.2.jar RAPTOR.java
java -Xmx28000M -Xms28000M -cp .:encog-core-3.2.0.jar:imgscalr-lib-4.2.jar RAPTOR
