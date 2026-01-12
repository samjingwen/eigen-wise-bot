#!/bin/bash

# Define paths relative to the project root
PROJECT_ROOT=$(pwd)
EXECUTABLE="$PROJECT_ROOT/util/tex2img"

# Loop from 1 to 5
for i in {0..5}; do
    INPUT_TEX="$PROJECT_ROOT/src/main/resources/quiz/$i/raw.tex"
    OUTPUT_IMG="$PROJECT_ROOT/src/main/resources/quiz/$i/img.jpg"

    # Check if input file exists before attempting conversion
    if [[ ! -f "$INPUT_TEX" ]]; then
        echo "Skipping $i: raw.tex not found."
        continue
    fi

    echo "Processing $i..."

    # Execute the command silently
    "$EXECUTABLE" --margins 30 --unit px "$INPUT_TEX" "$OUTPUT_IMG" > /dev/null 2>&1

    # Check exit code
    if [[ $? -eq 0 ]]; then
        echo "Successfully generated image for $i"
    else
        echo "Failed to generate image for $i. Exit code: $?"
    fi
done