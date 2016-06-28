#!/bin/bash

path_to_external_jar="$(pwd)/json-20160212.jar"

if [ $1 == "clean" ]; then
    rm *.class
    rm "$(pwd)/results.txt"
elif [ $1 == "match" ]; then 
    javac -cp $path_to_external_jar MatchProduct.java
    java -cp .:$path_to_external_jar MatchProduct
fi
