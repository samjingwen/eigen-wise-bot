#!/bin/bash

# Define paths relative to the project root
PROJECT_ROOT=$(pwd)
EXECUTABLE="$PROJECT_ROOT/util/tex2img"

# Loop through modules and their ranges
for module in linalg advlinalg; do
    if [ "$module" = "linalg" ]; then
        start=0
        end=0
    else
        start=0
        end=5
    fi

    for ((i=start; i<=end; i++)); do
        INPUT_TEX="$PROJECT_ROOT/src/main/resources/quiz/$module/$i/raw.tex"
        OUTPUT_IMG="$PROJECT_ROOT/src/main/resources/quiz/$module/$i/img.jpg"

        # Check if input file exists before attempting conversion
        if [[ ! -f "$INPUT_TEX" ]]; then
            echo "Skipping $module/$i: raw.tex not found."
            continue
        fi

        echo "Processing $module/$i..."

        # Execute the command silently
        "$EXECUTABLE" --margins 30 --unit px "$INPUT_TEX" "$OUTPUT_IMG" > /dev/null 2>&1

        # Check exit code
        if [[ $? -eq 0 ]]; then
            echo "Successfully generated image for $module/$i"
        else
            echo "Failed to generate image for $module/$i. Exit code: $?"
        fi
    done
done