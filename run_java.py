import subprocess
import os
import sys

# Get the directory of the current script
script_dir = os.path.dirname(os.path.abspath(__file__))
os.chdir(script_dir)

print("Running gradle test...")
try:
    # Use gradle instead of mvn.cmd as the project uses Gradle
    result = subprocess.run(["gradle", "test"], capture_output=True, text=True, check=False)
    with open("gradle_output.txt", "w", encoding="utf-8") as f:
        f.write(result.stdout)
        f.write(result.stderr)
    print("Done. Saved to gradle_output.txt")
except Exception as e:
    print(f"Failed to run script: {e}")
