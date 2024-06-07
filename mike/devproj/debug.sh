#!/bin/bash

# Variables
SRC_FOLDER=$1      # Folder containing Java source files
CLASS_FOLDER=$2    # Folder containing dependency classes
OUT_FOLDER="out"   # Output folder
ENTRY_FILE=$3      # Entry point file (e.g., MainClass)

# Create the output folder if it doesn't exist
mkdir -p $OUT_FOLDER

# Copy class dependencies to the output folder
cp -r $CLASS_FOLDER/* $OUT_FOLDER/

# Find all Java source files
JAVA_FILES=$(find $SRC_FOLDER -name "*.java")

# Compile the Java source files to the output folder, including the dependency class folder in the classpath
javac -d $OUT_FOLDER -cp $OUT_FOLDER:$CLASS_FOLDER $JAVA_FILES

# Run the Java program using jdb for debugging, including the output folder in the classpath
jdb -classpath $OUT_FOLDER $ENTRY_FILE
