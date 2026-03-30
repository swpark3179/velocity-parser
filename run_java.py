import subprocess
import os
import sys

os.chdir(r"c:\Users\s-wpa\Desktop\velocity-parser")

print("Running mvn test...")
try:
    result = subprocess.run(["mvn.cmd", "test"], capture_output=True, text=True, check=False)
    with open("mvn_output.txt", "w", encoding="utf-8") as f:
        f.write(result.stdout)
        f.write(result.stderr)
    print("Done. Saved to mvn_output.txt")
except Exception as e:
    print(f"Failed to run script: {e}")
