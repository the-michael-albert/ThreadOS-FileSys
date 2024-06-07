# Variables
$SRC_FOLDER = $args[0]       # Folder containing Java source files
$CLASS_FOLDER = $args[1]     # Folder containing dependency classes
$OUT_FOLDER = "out"          # Output folder
$ENTRY_FILE = $args[2]       # Entry point file (e.g., MainClass)

# Create the output folder if it doesn't exist
if (-not (Test-Path -Path $OUT_FOLDER)) {
    New-Item -ItemType Directory -Path $OUT_FOLDER
}

# Copy class dependencies to the output folder
Copy-Item -Path "$CLASS_FOLDER\*" -Destination $OUT_FOLDER -Recurse

# Find all Java source files
$JAVA_FILES = Get-ChildItem -Path $SRC_FOLDER -Recurse -Filter *.java | ForEach-Object { $_.FullName }

# Compile the Java source files to the output folder, including the dependency class folder in the classpath
& javac -d $OUT_FOLDER -cp "$OUT_FOLDER;$CLASS_FOLDER" $JAVA_FILES

# Run the Java program using the entry file, including the output folder in the classpath
& java -cp $OUT_FOLDER $ENTRY_FILE
