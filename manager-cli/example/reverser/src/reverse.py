import os
import sys
import zipfile

def main():
    input_dir = "/app/in"
    output_dir = "/app/out"

    try:
        # Find the first .zip archive in the input directory
        input_archive_path = None
        for filename in os.listdir(input_dir):
            if filename.lower().endswith(".zip"):
                input_archive_path = os.path.join(input_dir, filename)
                break

        if not input_archive_path:
            print("Error: No .zip archive found in input directory.", file=sys.stderr)
            sys.exit(2)

        content_to_reverse = ""
        with zipfile.ZipFile(input_archive_path, 'r') as archive:
            for name in archive.namelist():
                if name.lower().endswith(".txt"):
                    with archive.open(name) as text_file:
                        content_to_reverse = text_file.read().decode('utf-8')
                        break

        # Perform the transformation
        reversed_content = content_to_reverse[::-1]

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