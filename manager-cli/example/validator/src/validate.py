import os
import sys
import shutil
import zipfile

def main():
    input_dir = "/app/in"
    output_dir = "/app/out"
    keyword = "quick"

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

        # Check content without fully extracting
        is_valid = False
        with zipfile.ZipFile(input_archive_path, 'r') as archive:
            for name in archive.namelist():
                if name.lower().endswith(".txt"):
                    with archive.open(name) as text_file:
                        content = text_file.read().decode('utf-8')
                        if keyword in content:
                            is_valid = True
                            break
                if is_valid:
                    break

        if is_valid:
            # Pass the original, untouched archive to the output
            shutil.copy(input_archive_path, output_dir)
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