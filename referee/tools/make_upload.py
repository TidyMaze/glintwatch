"""Build the CodinGame upload zip from the live tree: config/, src/main/ and an upload pom.

The upload pom is the live pom without test-only parts (junit dependency, <build> plugins: surefire, jacoco),
the shape of community games that run on CG (e.g. Ataxx: gameengine 4.7.8, Java 17, no plugins). Tests stay in
the repo: CG's builder would otherwise compile and run them. Usage: python3 -I tools/make_upload.py <zip path>
"""
import os
import re
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..")


def upload_pom(pom):
    out = re.sub(r"\s*<dependency>\s*<groupId>org\.junit[^<]*</groupId>.*?</dependency>", "", pom, flags=re.S)
    out = re.sub(r"\s*<build>.*?</build>", "", out, flags=re.S)
    return out


def check(pom, names):
    errors = []
    for bad in ("junit", "jacoco", "surefire", "<build>"):
        if bad in pom:
            errors.append("upload pom still has " + bad)
    if "<maven.compiler.target>17</maven.compiler.target>" not in pom:
        errors.append("upload pom must target Java 17 (gameengine 4.7.8 classes are Java 17)")
    for n in names:
        if n.startswith("src/test/") or "/target/" in n or n.endswith("manifest.json"):
            errors.append("unexpected file " + n)
    for need in ("pom.xml", "config/config.ini", "config/stub.txt", "src/main/java/com/codingame/game/Referee.java"):
        if need not in names:
            errors.append("missing " + need)
    return errors


def main(dest):
    pom = upload_pom(open(os.path.join(ROOT, "pom.xml"), encoding="utf-8").read())
    entries = {"pom.xml": pom.encode("utf-8")}
    for top in ("config", os.path.join("src", "main")):
        for dirpath, _, files in os.walk(os.path.join(ROOT, top)):
            for f in sorted(files):
                full = os.path.join(dirpath, f)
                rel = os.path.relpath(full, ROOT).replace(os.sep, "/")
                if f == ".DS_Store" or rel.endswith("manifest.json"):
                    continue
                entries[rel] = open(full, "rb").read()
    errors = check(pom, entries)
    if errors:
        raise SystemExit("\n".join(errors))
    with zipfile.ZipFile(dest, "w", zipfile.ZIP_DEFLATED) as z:
        for name in sorted(entries):
            z.writestr(name, entries[name])
    print(dest, len(entries), "files", os.path.getsize(dest), "bytes")


if __name__ == "__main__":
    main(sys.argv[1])
