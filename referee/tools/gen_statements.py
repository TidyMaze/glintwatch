"""Generate config/levelK/statement_en.html from the "Statement v4" block of ../DESIGN.md.

League tags (DESIGN statement v4): a tag on a ### heading scopes the whole section; at the start of a paragraph,
the whole paragraph; elsewhere (list item, mid-line) up to the next tag, a [/], the close of an enclosing (), or
the end of the sentence. Levels: 1 Wood 2, 2 Wood 1, 3 Bronze, 4 Silver.
Usage: python3 -I tools/gen_statements.py [DESIGN.md] [config dir]
"""
import html
import os
import re
import sys

TAGS = {
    "Wood 2 only": {1}, "Wood only": {1, 2}, "below Silver": {1, 2, 3}, "Wood 1+": {2, 3, 4},
    "Bronze+": {3, 4}, "Silver+": {4}, "Wood 1 only": {2}, "Bronze only": {3},
}
TAG_RE = re.compile(r"\[(" + "|".join(re.escape(t) for t in TAGS) + r")\]")
CLOSE = "[/]"
GAP = "\x00"  # marks where hidden text was removed
NEW_OPEN, NEW_CLOSE, NEW_BLOCK = "\x01", "\x02", "\x03"
NEW_STYLE = "color:#7cc576;background-color:rgba(124,197,118,.1);padding:2px"
OPENERS = {"Wood 2 only", "Wood 1 only", "Bronze only"}


def is_new(tag, level):
    """A rule is boxed as new only in the first league where it appears (designer convention c)."""
    return level > 1 and tag not in OPENERS and min(TAGS[tag]) == level


def _sentence_end(text, start):
    """Index just past the first '.' that ends a sentence (followed by whitespace or end), outside backticks."""
    code = False
    for i in range(start, len(text)):
        ch = text[i]
        if ch == "`":
            code = not code
        elif ch == "." and not code and (i + 1 == len(text) or text[i + 1] in " \n"):
            return i + 1
    return len(text)


def filter_inline(text, level):
    """Apply inline tags of one unit (list item or plain text, may span lines) for a level."""
    out = []
    pos = 0
    while True:
        m = TAG_RE.search(text, pos)
        if not m:
            out.append(text[pos:].replace(CLOSE, ""))
            break
        before = text[pos:m.start()]
        start = m.end()
        # "word [Tag] text": drop the space after the tag; "word[Tag] text": the space belongs to the scope
        if start < len(text) and text[start] == " " and (before == "" or before.endswith((" ", "\n", "("))):
            start += 1
        # scope end candidates
        nxt = TAG_RE.search(text, start)
        end = nxt.start() if nxt else len(text)
        close = text.find(CLOSE, start)
        closed = False
        if 0 <= close < end:
            end, closed = close, True
        depth, paren_end = 0, -1
        for i in range(start, end):
            if text[i] == "(":
                depth += 1
            elif text[i] == ")":
                if depth == 0:
                    paren_end = i
                    break
                depth -= 1
        if paren_end >= 0:
            end, closed = paren_end, False
        sent = _sentence_end(text, start)
        if paren_end < 0 and not (0 <= close < (nxt.start() if nxt else len(text))) and sent < end:
            end = sent
        visible = level in TAGS[m.group(1)]
        scope = text[start:end]
        if visible:
            out.append(before)
            out.append(NEW_OPEN + scope + NEW_CLOSE if is_new(m.group(1), level) and scope.strip() else scope)
        else:
            if paren_end >= 0 and before.endswith("("):
                before = before[:-1].rstrip(" ")
                end = paren_end + 1  # drop the closing paren too
            elif paren_end >= 0:
                before = re.sub(r",\s*$", "", before)
            elif scope.rstrip().endswith(".") and re.search(r",\s*$", before):
                before = re.sub(r",\s*$", ".", before)  # "a, b, [hidden c.] D" -> "a, b. D"
            out.append(before + GAP)
        pos = end + (len(CLOSE) if closed else 0)
    s = "".join(out)
    # clean only where text was removed
    s = re.sub(" *" + GAP + " *(?=[.,;)\n]|$)", "", s)
    s = re.sub(" *" + GAP + " *", " ", s)
    return "\n".join(line.rstrip() for line in s.split("\n"))


def _units(lines):
    """Split a block into units: list items (with indented continuation lines) and plain-text runs."""
    units, cur = [], None
    for line in lines:
        item = re.match(r"^\s*- ", line)
        cont = line.startswith("  ") and not item
        if item:
            cur = [line]
            units.append(cur)
        elif cont and cur is not None and re.match(r"^\s*- ", cur[0]):
            cur.append(line)
        elif cur is not None and not re.match(r"^\s*- ", cur[0]) and not line.startswith("|") and not line.startswith("#"):
            cur.append(line)
        else:
            cur = [line]
            units.append(cur)
    return units


def level_markdown(doc, level):
    lines = [re.sub(r"^> ?", "", l) for l in doc.split("\n")]
    blocks, cur = [], []
    for l in lines:
        if l.strip() == "":
            if cur:
                blocks.append(cur)
            cur = []
        else:
            cur.append(l)
    if cur:
        blocks.append(cur)
    # a heading is its own block, so a tag at the start of the next line scopes that paragraph
    split = []
    for b in blocks:
        if b[0].startswith("#") and len(b) > 1:
            split.extend([[b[0]], b[1:]])
        else:
            split.append(b)
    blocks = split
    out, skip_section, new_section = [], False, False
    for block in blocks:
        new_block = False
        head = re.match(r"^(#+) (?:\[([^\]]+)\] )?(.*)$", block[0])
        if head and head.group(1) == "###":
            tag = head.group(2)
            skip_section = tag in TAGS and level not in TAGS[tag]
            if skip_section:
                continue
            new_section = tag in TAGS and is_new(tag, level)
            block = ["### " + head.group(3)] + block[1:]
            if new_section:
                out.append(NEW_BLOCK + "\n" + block[0])
                continue
        elif skip_section:
            continue
        lead = TAG_RE.match(block[0])
        if lead:
            if level not in TAGS[lead.group(1)]:
                continue
            new_block = is_new(lead.group(1), level)
            block = [block[0][lead.end():].lstrip(" ")] + block[1:]
        kept = []
        for unit in _units(block):
            text = filter_inline("\n".join(unit), level)
            if re.match(r"^\s*-\s*$", text) or text.strip() == "":
                continue
            kept.append(text)
        kept = [k for k in kept if k.strip()]
        if kept:
            text = "\n".join(kept).strip("\n")
            if new_block or new_section:
                text = NEW_BLOCK + "\n" + text.replace(NEW_OPEN, "").replace(NEW_CLOSE, "")
            out.append(text)
    return "\n\n".join(out)


ACTIONS = {"PLACE", "TURN", "WAIT"}
# `N` is the board size (a variable); as a direction it only appears in the list "`E`, `S`, `W` or `N`"
SYMBOLS = {".", "#", "/", "\\", "E", "S", "W", "A", "B", "C", "D", "1", "2", "3"}
DIRECTIONS = "`E`, `S`, `W` or `N`"
IMAGE_BASE = "https://raw.githubusercontent.com/TidyMaze/glintwatch/main/art/png/statement"


def _token(t):
    e = html.escape(t, quote=False)
    return "<const>%s</const>" % e if re.fullmatch(r"-?\d+", t) or t in SYMBOLS else "<var>%s</var>" % e


def _code(c):
    """Moderator point 1: commands -> <action>, numbers and map symbols -> <const>, variables -> <var>."""
    toks = c.split()
    if toks and toks[0] in ACTIONS:
        return " ".join(["<action>%s</action>" % toks[0]] + [_token(t) for t in toks[1:]])
    if len(toks) == 1:
        return _token(toks[0])
    return "<var>%s</var>" % html.escape(c, quote=False)


def _inline_html(s):
    if DIRECTIONS in s:
        return DIRECTIONS.join(_inline_html(x) for x in s.split(DIRECTIONS)).replace(
            DIRECTIONS, "<const>E</const>, <const>S</const>, <const>W</const> or <const>N</const>")
    parts = re.split(r"(`[^`]*`)", s)
    res = []
    for p in parts:
        if p.startswith("`") and p.endswith("`") and len(p) >= 2:
            res.append(_code(p[1:-1]))
        else:
            e = html.escape(p, quote=False)
            e = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", e)
            e = re.sub(r"https://github\.com/[\w./-]+", lambda m: '<a href="%s" target="_blank">GitHub</a>' % m.group(0), e)
            res.append(e)
    return "".join(res).replace(NEW_OPEN, '<span style="%s">' % NEW_STYLE).replace(NEW_CLOSE, "</span>")


def _io_line(text):
    """'Line 1: ...' / 'Next ... lines: ...' -> statement-lineno prefix (moderator point 5)."""
    m = re.match(r"^((?:Line \d+|Next [^:]*?):\s)(.*)$", text, re.S)
    if not m:
        return "<p>%s</p>" % _inline_html(text)
    return '<p><span class="statement-lineno">%s</span>%s</p>' % (_inline_html(m.group(1)), _inline_html(m.group(2)))


def _constraint(text):
    """One constraint per line; bare numbers are <const>."""
    out = []
    for part in re.split(r"(<[^>]+>[^<]*</[^>]+>)", _inline_html(text)):
        out.append(part if part.startswith("<") else re.sub(r"(?<![\w.])(\d+)(?![\w.])", r"<const>\1</const>", part))
    return "".join(out) + "<br>"


def _http_status(url):
    import urllib.request
    req = urllib.request.Request(url, method="HEAD")
    with urllib.request.urlopen(req, timeout=10) as r:
        return r.status


def url_live(name, fetch=_http_status):
    """An image is embedded only when its public URL answers 200 (never ship a broken image)."""
    try:
        return fetch(IMAGE_BASE + "/" + name) == 200
    except Exception:
        return False


def image_html(line, exists=url_live):
    """'![alt](img/name.png)' -> <img> on the public repo; dropped while the PNG does not exist (point 4)."""
    m = re.match(r"^!\[([^\]]*)\]\((?:[^)]*/)?([^/)]+)\)\s*$", line.strip())
    if not m or not exists(m.group(2)):
        return ""
    return '<img src="%s/%s" alt="%s" style="width: 100%%; max-width: 400px; display: block; margin: 10px auto;" />' % (
        IMAGE_BASE, m.group(2), html.escape(m.group(1), quote=True))


def to_html(md):
    out = []
    for block in md.split("\n\n"):
        lines = block.split("\n")
        if lines[0] == NEW_BLOCK:
            out.append('<div style="%s">\n%s\n</div>' % (NEW_STYLE, to_html("\n".join(lines[1:]))))
            continue
        i = 0
        while i < len(lines):
            line = lines[i]
            if line.startswith("### "):
                out.append("<h2>" + _inline_html(line[4:]) + "</h2>")
                i += 1
            elif line.lstrip().startswith("!["):
                img = image_html(line)
                if img:
                    out.append(img)
                i += 1
            elif line.startswith("|"):
                rows = []
                while i < len(lines) and lines[i].startswith("|"):
                    if not re.match(r"^\|[-| ]+\|$", lines[i]):
                        rows.append([c.strip() for c in lines[i].strip("|").split("|")])
                    i += 1
                t = ["<table>"]
                for r, cells in enumerate(rows):
                    tag = "th" if r == 0 else "td"
                    t.append("<tr>" + "".join("<%s>%s</%s>" % (tag, _inline_html(c), tag) for c in cells) + "</tr>")
                t.append("</table>")
                out.append("\n".join(t))
            elif re.match(r"^- ", line):
                items = []
                while i < len(lines) and (re.match(r"^- ", lines[i]) or lines[i].startswith("  ")):
                    if re.match(r"^- ", lines[i]):
                        items.append([lines[i][2:], []])
                    elif re.match(r"^\s+- ", lines[i]):
                        items[-1][1].append(lines[i].strip()[2:])
                    else:
                        items[-1][0] += " " + lines[i].strip()
                    i += 1
                u = ["<ul>"]
                for text, subs in items:
                    sub = "" if not subs else "\n<ul>\n" + "\n".join("<li>" + _inline_html(x) + "</li>" for x in subs) + "\n</ul>\n"
                    u.append("<li>" + _inline_html(text) + sub + "</li>")
                u.append("</ul>")
                out.append("\n".join(u))
            else:
                para = []
                while i < len(lines) and not lines[i].startswith(("### ", "|", "- ")) and not lines[i].lstrip().startswith("!["):
                    para.append(lines[i].strip())
                    i += 1
                out.append("<p>" + _inline_html(" ".join(para)) + "</p>")
    return "\n".join(out)


COVER_URL = "https://raw.githubusercontent.com/TidyMaze/glintwatch/main/art/cover.png"


def read_pitch(description):
    """The one-line pitch: first line under '## Pitch' in story/DESCRIPTION.md."""
    m = re.search(r"^## Pitch.*?\n(.+?)\n", description, re.M)
    return m.group(1).strip() if m else ""


H3 = '<h3 style="font-size: 14px; font-weight: 700; padding-top: 15px; color: #838891; padding-bottom: 15px">%s</h3>'
LEAGUE_IMG = "//cdn.codingame.com/smash-the-code/statement/league_wood_04.png"
IO_TITLES = {"Initialization input": "Initialization Input", "Input for one game turn": "Input for one game turn",
             "Output for one game turn": "Output for one game turn", "Constraints": "Constraints"}


def _sections(md):
    """[(title, is_new, [blocks])] in order; blocks keep their NEW_BLOCK markers."""
    out = []
    for block in md.split("\n\n"):
        block = block.strip("\n")  # a hidden last line leaves a blank line before the next heading
        if not block:
            continue
        lines = block.split("\n")
        new = lines[0] == NEW_BLOCK
        head = lines[1] if new and len(lines) > 1 else lines[0]
        if head.startswith("### "):
            out.append([head[4:], new, []])
        elif out:
            out[-1][2].append(block)
        else:
            out.append(["", False, [block]])
    return out


def _strip(block):
    return block[len(NEW_BLOCK) + 1:] if block.startswith(NEW_BLOCK + "\n") else block


def _items(blocks):
    """Top-level '- ' list items of the blocks (continuation lines joined)."""
    items = []
    for b in blocks:
        for line in _strip(b).split("\n"):
            if line.startswith("- "):
                items.append(line[2:])
            elif items and line.strip():
                items[-1] += " " + line.strip()
    return items


def _protocol(blocks):
    out = []
    for block in blocks:
        lines = _strip(block).split("\n")
        m = re.match(r"^\*\*(.+?)\*\*\s*$", lines[0])
        if not m:
            out.append(to_html(block))
            continue
        title, body = m.group(1), lines[1:]
        text = []
        if title == "Constraints":
            text = [_constraint(item) for item in _items([ "\n".join(body)])]
        else:
            cur, subs = None, []

            def flush():
                if cur is not None:
                    text.append(_io_line(cur))
                    if subs:
                        text.append("<ul>\n" + "\n".join("<li>%s</li>" % _inline_html(x) for x in subs) + "\n</ul>")

            for line in body:
                if line.startswith("- "):
                    flush()
                    cur, subs = line[2:], []
                elif re.match(r"^\s+- ", line):
                    subs.append(line.strip()[2:])
                elif line.strip():
                    if subs:
                        subs[-1] += " " + line.strip()
                    else:
                        cur = (cur + " " if cur else "") + line.strip()
            flush()
        out.append('<div class="blk"><div class="title">%s</div><div class="text">\n%s\n</div></div>' % (
            IO_TITLES.get(title, title), "\n".join(text)))
    return "\n".join(out)


def page(doc, level, cover=None, pitch=None):
    """Whole statement page for a level, CG layout per STATEMENT-HTML-SPEC.md (moderator review)."""
    secs = _sections(level_markdown(doc, level))
    by = {t: (new, blocks) for t, new, blocks in secs}
    out = ['<div id="statement_back" class="statement_back" style="display: none"></div>', '<div class="statement-body">']
    if cover:
        out.append('<div class="statement-section statement-cover" style="text-align:center">')
        out.append('<img src="%s" alt="Glintwatch" style="max-width:100%%">' % html.escape(cover, quote=True))
        if pitch:
            out.append("<p><em>" + html.escape(pitch, quote=False) + "</em></p>")
        out.append("</div>")
    if "League introduction" in by:
        paras = [_strip(b) for b in by["League introduction"][1]]
        generic = " ".join(p for p in paras if "**This league:**" not in p).replace("\n", " ")
        first, _, rest = generic.partition(". ")
        this = [p.replace("\n", " ") for p in paras if "**This league:**" in p]
        out.append('<div style="color:#7cc576;background-color:rgba( 124 , 197 , 118 , 0.1 );padding:20px;margin-right:15px;'
                   'margin-left:15px;margin-bottom:10px;text-align:left">')
        out.append('<div style="text-align:center;margin-bottom:6px"><img src="%s"></div>' % LEAGUE_IMG)
        out.append('<p style="text-align:center;font-weight:700;margin-bottom:6px">%s.</p>' % _inline_html(first).replace("<strong>", "<b>").replace("</strong>", "</b>"))
        content = _inline_html(rest)
        if this:
            content += "<br><br>" + "<br>".join(_inline_html(t) for t in this)
        out.append('<div class="statement-league-alert-content">%s</div>' % content.replace("<strong>", "<b>").replace("</strong>", "</b>"))
        out.append("</div>")
    if "Goal" in by:
        out.append('<div class="statement-section statement-goal">')
        out.append('<h2><span class="icon icon-goal">&nbsp;</span><span>Goal</span></h2>')
        out.append('<div class="statement-goal-content">%s</div>' % to_html("\n\n".join(by["Goal"][1])))
        out.append("</div>")
    special = {"League introduction", "Goal", "Game input", "Technical details", "League preview", ""}
    out.append('<div class="statement-section statement-rules">')
    out.append('<h2><span class="icon icon-rules">&nbsp;</span><span>Rules</span></h2>')
    out.append('<div class="statement-rules-content">')
    for title, new, blocks in secs:
        if title in special:
            continue
        if title in ("Victory conditions", "Defeat conditions"):
            cls, icon, name = (("statement-victory-conditions", "victory", "Victory Conditions") if title.startswith("Victory")
                               else ("statement-lose-conditions", "lose", "Defeat Conditions"))
            lis = "".join("<li>%s</li>" % _inline_html(x) for x in _items(blocks))
            out.append('<div class="%s"><div class="icon %s"></div><div class="blk"><div class="title">%s</div>'
                       '<div class="text"><ul style="padding-bottom: 0;">%s</ul></div></div></div>' % (cls, icon, name, lis))
            continue
        body = H3 % _inline_html(title) + "\n" + to_html("\n\n".join(blocks))
        out.append('<div style="%s">\n%s\n</div>' % (NEW_STYLE, body) if new else body)
    if "Technical details" in by:
        lis = "".join("<li>%s</li>" % _inline_html(x) for x in _items(by["Technical details"][1]))
        out.append("<br />")
        out.append('<div class="statement-section statement-expertrules">')
        out.append('<h2><span class="icon icon-expertrules">&nbsp;</span><span>Technical Details</span></h2>')
        out.append('<div class="statement-expert-rules-content"><ul style="padding-left: 20px; padding-bottom: 0">%s</ul></div>' % lis)
        out.append("</div>")
    out.append("</div>")
    out.append("</div>")
    if "Game input" in by:
        out.append('<div class="statement-section statement-protocol">')
        out.append('<h2><span class="icon icon-protocol">&nbsp;</span><span>Game Input/Output</span></h2>')
        out.append(_protocol(by["Game input"][1]))
        out.append("</div>")
    preview = [_strip(b).replace("\n", " ").strip() for b in by.get("League preview", (False, []))[1]]
    preview = [p for p in preview if p]
    if preview:
        out.append('<div style="color: #7cc576; background-color: rgba(124, 197, 118,.1); padding: 20px; margin-top: 10px; text-align: left;">')
        out.append('<div style="text-align: center; margin-bottom: 6px"><img src="%s" /></div>' % LEAGUE_IMG)
        out.append('<div style="text-align: center; font-weight: 700; margin-bottom: 6px;">What is in store for me in the higher leagues?</div>')
        out.append("<ul>%s</ul>" % "".join("<li>%s</li>" % _inline_html(p) for p in preview))
        out.append("</div>")
    out.append("</div>")
    return "\n".join(out) + "\n"


def statement_block(design_text):
    m = re.search(r"^## Statement v4.*?$(.*?)^# ", design_text, re.S | re.M)
    if not m:
        raise SystemExit("Statement v4 block not found")
    quoted = [l for l in m.group(1).split("\n") if l.startswith(">")]
    return "\n".join(quoted)


def main(argv):
    here = os.path.dirname(os.path.abspath(__file__))
    design = argv[1] if len(argv) > 1 else os.path.join(here, "..", "..", "DESIGN.md")
    config = argv[2] if len(argv) > 2 else os.path.join(here, "..", "config")
    doc = statement_block(open(design, encoding="utf-8").read())
    pitch = read_pitch(open(os.path.join(os.path.dirname(design), "story", "DESCRIPTION.md"), encoding="utf-8").read())
    for level in range(1, 5):
        text = page(doc, level, cover=COVER_URL, pitch=pitch)
        path = os.path.join(config, "level%d" % level, "statement_en.html")
        with open(path, "w", encoding="utf-8") as f:
            f.write(text)
        print(path, len(text), "chars")


if __name__ == "__main__":
    main(sys.argv)
