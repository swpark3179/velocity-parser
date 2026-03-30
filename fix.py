import os
path = r"src\test\java\com\velocityparser\TestTemplates.java"
with open(path, "r", encoding="utf-8") as f:
    text = f.read()

text = text.replace("!= null", "!= ''")

with open(path, "w", encoding="utf-8") as f:
    f.write(text)

print("Replaced nulls")
