import os
import sys

def main():
    input_dir = "/app/in"
    output_dir = "/app/out"

    try:
        # find the first file in the input directory recursively
        input_path = None
        for root, dirs, files in os.walk(input_dir):
            for filename in files:
                input_path = os.path.join(root, filename)
                break
            
        if input_path is None:
            print("No text file found in the input directory.", file=sys.stderr)
            sys.exit(1)
            
        # Read the content of the text file
        with open(input_path, 'r') as file:
            content = file.read()

        # Perform the transformation
        reversed_content = content[::-1]

        # Write the result to a new file in the output directory
        with open(os.path.join(output_dir, "reversed.txt"), 'w') as f:
            f.write(reversed_content)

        print("Transformation successful: Text has been reversed.")
        sys.exit(0)

    except Exception as e:
        print(f"An unexpected error occurred in reverser plugin: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()