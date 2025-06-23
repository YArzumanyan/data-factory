import os
import sys
import shutil

def main():
    input_dir = "/app/in"
    output_dir = "/app/out"
    keyword = "quick"

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

        if keyword in content:
            # Pass the original, untouched file to the output
            shutil.copy(input_path, output_dir)
            print(f"Validation successful: Keyword '{keyword}' found.")
            sys.exit(0)
        else:
            print(f"Validation failed: Keyword '{keyword}' not found.", file=sys.stderr)
            sys.exit(1)

    except Exception as e:
        print(f"An unexpected error occurred in validator plugin: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()