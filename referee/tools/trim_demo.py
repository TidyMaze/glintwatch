"""Trim the intro replay (src/main/resources/view/demo.js, written by the runner's SET INTRO REPLAY) to its first
N turns. Frames only hold changes, so a prefix of the frame list is a valid replay, BUT the end screen gets its
title sprite only in the last frame: a trimmed demo logs 'Could not find image: "undefined"' (seen 2026-11-11), so
the shipped demo is the full game.
Usage: python3 -I tools/trim_demo.py <turns>"""
import json
import os
import re
import sys

PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "view", "demo.js")


def trim(demo, turns):
    keep, keys = [], 0
    for v in demo["views"]:
        if re.match(r"KEY_FRAME \d+", v):
            keys += 1  # one KEY_FRAME per turn, frame 0 = init
            if keys > turns + 1:
                break
        keep.append(v)
    demo["views"] = keep
    return demo


def main(turns):
    text = open(PATH, encoding="utf-8").read()
    demo = json.loads(text[text.index("=") + 1:text.rindex(";")])
    demo = trim(demo, turns)
    out = "export const demo = \n" + json.dumps(demo, separators=(",", ":")) + ";\n"
    open(PATH, "w", encoding="utf-8").write(out)
    print(PATH, len(demo["views"]), "views", len(out.encode("utf-8")), "bytes")


if __name__ == "__main__":
    main(int(sys.argv[1]))
