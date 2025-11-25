#!/bin/bash
set -e
echo "Compiling Java files..."
mkdir -p out
javac -d out src/*.java
echo "Compiled -> out/"
