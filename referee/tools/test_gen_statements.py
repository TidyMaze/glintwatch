"""Tests for gen_statements.py. Run: python3 -I tools/test_gen_statements.py"""
import os, re, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_statements as G

def strip(t):
    return t.replace("\x01", "").replace("\x02", "").replace("\x03\n", "")

def fi(text, level):
    return strip(G.filter_inline(text, level))

def md(doc, level):
    return strip(G.level_markdown(doc, level))

def check(name, got, want):
    if got != want:
        print("FAIL", name, "\n got:", repr(got), "\nwant:", repr(want)); sys.exit(1)

# tag -> levels (1 Wood 2, 2 Wood 1, 3 Bronze, 4 Silver)
check("levels", [sorted(G.TAGS[t]) for t in ["Wood 2 only", "Wood only", "below Silver", "Wood 1+", "Bronze+", "Silver+", "Wood 1 only", "Bronze only"]],
      [[1], [1, 2], [1, 2, 3], [2, 3, 4], [3, 4], [4], [2], [3]])

# inline: sentence scope, next tag, [/], parentheses
check("sentence", fi("- [Wood 1+] `#` rock: a beam stops. [Wood 2 only] There are no rocks.", 1), "- There are no rocks.")
check("sentence2", fi("- [Wood 1+] `#` rock: a beam stops. [Wood 2 only] There are no rocks.", 3), "- `#` rock: a beam stops.")
check("closer", fi("after [Wood 2 only] 100 [Wood 1+] 150[/] turns, or when.", 1), "after 100 turns, or when.")
check("closer2", fi("after [Wood 2 only] 100 [Wood 1+] 150[/] turns, or when.", 2), "after 150 turns, or when.")
check("paren", fi("`x y value phase` ([below Silver] `phase` is -1: always awake)", 4), "`x y value phase`")
check("paren2", fi("`x y value phase` ([below Silver] `phase` is -1: always awake)", 1), "`x y value phase` (`phase` is -1: always awake)")
check("list in parens", fi("not empty (rock, crystal, mirror, [Bronze+] pending mirror), next", 2), "not empty (rock, crystal, mirror), next")
check("trailing", fi("else 0 [Wood only] (always 0 in this league)", 3), "else 0")
check("mid", fi("1. [Bronze+] Supply (before the input is sent). 2. All placements.", 2), "1. 2. All placements.")

# block level: heading section, paragraph, sentence across a wrapped line
doc = """> Intro.
>
> [Wood 2 only] Night falls.
> Second line of the same paragraph.
>
> ### [Bronze+] Mirror economy
> - **Supply.** text
>
> ### Goal
> Score more, [Wood 1+] turning where there is no active mirror or the
> lock is not 0. A line that is not valid eliminates you."""
w2 = md(doc, 1)
check("w2", w2, "Intro.\n\nNight falls.\nSecond line of the same paragraph.\n\n### Goal\n\nScore more. A line that is not valid eliminates you.")
br = md(doc, 3)
check("bronze", br, "Intro.\n\n### Mirror economy\n\n- **Supply.** text\n\n### Goal\n\nScore more, turning where there is no active mirror or the\nlock is not 0. A line that is not valid eliminates you.")

# markdown -> html
check("html", G.to_html("### Goal\nScore `x` **bold**.\n\n- a\n- b\n\n| A | B |\n|---|---|\n| 1 | 2 |"),
      "<h2>Goal</h2>\n<p>Score <var>x</var> <strong>bold</strong>.</p>\n<ul>\n<li>a</li>\n<li>b</li>\n</ul>\n"
      "<table>\n<tr><th>A</th><th>B</th></tr>\n<tr><td>1</td><td>2</td></tr>\n</table>")
check("escape", G.to_html("a < b & `c > d`"), "<p>a &lt; b &amp; <var>c &gt; d</var></p>")
check("indent kept", fi("  - `type`: 0 or 1", 1), "  - `type`: 0 or 1")
check("ellipsis kept", fi("Your beam goes (1,3) ... (5,3), turns.", 1), "Your beam goes (1,3) ... (5,3), turns.")
check("comma to stop", fi("no mirror in hand, [Wood 1+] turning where the lock is not 0. A line", 1), "no mirror in hand. A line")
check("nested html", G.to_html("- Next lines: `x y`\n  - `type`: 0\n  - `lock`: 1"),
      "<ul>\n<li>Next lines: <var>x y</var>\n<ul>\n<li><var>type</var>: 0</li>\n<li><var>lock</var>: 1</li>\n</ul>\n</li>\n</ul>")
check("heading then tagged paragraph", md("> ### Scoring\n> [Wood 2 only] Shared. All score it.\n>\n> [Wood 1+] Streaks.", 4), "### Scoring\n\nStreaks.")
check("glued tag keeps its space", fi("owner may place[Wood 1+] or turn[/] mirrors", 2), "owner may place or turn mirrors")
check("glued tag hidden", fi("owner may place[Wood 1+] or turn[/] mirrors", 1), "owner may place mirrors")
check("glued paren", fi("1 (`\\`)[Bronze+]; -1 for pending", 2), "1 (`\\`)")
check("glued paren shown", fi("1 (`\\`)[Bronze+]; -1 for pending", 3), "1 (`\\`); -1 for pending")

# new-rule box: only in the first league where the rule appears (not for openers, not in level 1)
NEW = G.NEW_STYLE
doc2 = "> Base rule.\n>\n> [Bronze+] Supply rule.\n>\n> - [Wood 1+] `TURN x y`: flip.\n> - `WAIT`."
check("new block", G.to_html(G.level_markdown(doc2, 3)),
      '<p>Base rule.</p>\n<div style="%s">\n<p>Supply rule.</p>\n</div>\n<ul>\n<li><action>TURN</action> <var>x</var> <var>y</var>: flip.</li>\n<li><action>WAIT</action>.</li>\n</ul>' % NEW)
check("new inline", G.to_html(G.level_markdown(doc2, 2)),
      '<p>Base rule.</p>\n<ul>\n<li><span style="%s"><action>TURN</action> <var>x</var> <var>y</var>: flip.</span></li>\n<li><action>WAIT</action>.</li>\n</ul>' % NEW)
check("no box later", "style" not in G.to_html(G.level_markdown(doc2, 4)), True)
check("opener plain", "style" not in G.to_html(G.level_markdown("> [Bronze only] Story.", 3)), True)
# cover + pitch at the top of every level
top = G.page("> Intro.", 2, cover="https://x/cover.png", pitch="Bend & hold.")
check("cover", top.split("\n")[1:5], ['<div class="statement-body">',
      '<div class="statement-section statement-cover" style="text-align:center">',
      '<img src="https://x/cover.png" alt="Glintwatch" style="max-width:100%">',
      '<p><em>Bend &amp; hold.</em></p>'])
check("pitch from description", G.read_pitch("# T\n\n## Pitch (<= 120 chars)\nShort line. 2 to 4 players.\n\n## Next\nx"), "Short line. 2 to 4 players.")

# moderator review (STATEMENT-HTML-SPEC.md): inline tags
check("action", G._inline_html("`PLACE x y type`"), "<action>PLACE</action> <var>x</var> <var>y</var> <var>type</var>")
check("action numbers", G._inline_html("`PLACE 5 3 1`"), "<action>PLACE</action> <const>5</const> <const>3</const> <const>1</const>")
check("const number", G._inline_html("`-1` and `30`"), "<const>-1</const> and <const>30</const>")
check("const symbols", G._inline_html("`#` `/` `\\` `E` `A` `2`"), "<const>#</const> <const>/</const> <const>\\</const> <const>E</const> <const>A</const> <const>2</const>")
check("directions const", G._inline_html("(`E`, `S`, `W` or `N`)"), "(<const>E</const>, <const>S</const>, <const>W</const> or <const>N</const>)")
check("var", G._inline_html("`playerCount` `N = 13` `s_i`"), "<var>playerCount</var> <var>N = 13</var> <var>s_i</var>")
check("link", G._inline_html("on GitHub: https://github.com/TidyMaze/glintwatch"),
      'on GitHub: <a href="https://github.com/TidyMaze/glintwatch" target="_blank">GitHub</a>')
check("lineno", G._io_line("Next `N` lines: a string `row`"), '<p><span class="statement-lineno">Next <var>N</var> lines: </span>a string <var>row</var></p>')
check("lineno plain", G._io_line("Tokens are separated by spaces."), "<p>Tokens are separated by spaces.</p>")
check("constraint", G._constraint("2 ≤ `playerCount` ≤ 4"), "<const>2</const> ≤ <var>playerCount</var> ≤ <const>4</const><br>")
check("constraint ms", G._constraint("Response time for one turn ≤ 50 ms"), "Response time for one turn ≤ <const>50</const> ms<br>")
check("image base is the artist's folder", G.IMAGE_BASE, "https://raw.githubusercontent.com/TidyMaze/glintwatch/main/art/png/statement")
seen = []
check("live check uses the public URL", G.url_live("mirrors.png", fetch=lambda u: seen.append(u) or 404), False)
check("live check url", seen, [G.IMAGE_BASE + "/mirrors.png"])
check("live check 200", G.url_live("mirrors.png", fetch=lambda u: 200), True)
check("live check offline", G.url_live("mirrors.png", fetch=lambda u: (_ for _ in ()).throw(OSError("offline"))), False)
check("image absent dropped", G.image_html("![Two mirrors](img/none.png)", lambda n: False), "")
check("image present", G.image_html("![Two mirrors](img/mirrors.png)", lambda n: True),
      '<img src="%s/mirrors.png" alt="Two mirrors" style="width: 100%%; max-width: 400px; display: block; margin: 10px auto;" />' % G.IMAGE_BASE)

check("image caption", G.image_html('![Lock](img/lock.png "A turned mirror stays locked 5 turns.")', lambda n: True),
      '<img src="%s/lock.png" alt="Lock" style="width: 100%%; max-width: 400px; display: block; margin: 10px auto 2px;" />'
      '<div style="text-align: center; font-size: 90%%; font-style: italic; margin-bottom: 10px;">A turned mirror stays locked 5 turns.</div>' % G.IMAGE_BASE)
G.image_html.__defaults__ = (lambda n: False,)
check("new box around a dropped image is dropped", G.to_html("\x03\n![Lock](img/lock.png)"), "")
G.image_html.__defaults__ = (G.url_live,)
check("image caption absent dropped", G.image_html('![Lock](img/lock.png "Caption")', lambda n: False), "")

check("adjacent hidden scopes leave no space", fi("x [Wood 2 only]a[/][Wood 1 only]b[/][Bronze only]c[/][Silver+](d)[/]);", 1), "x a);")
check("adjacent hidden scopes, middle", fi("x [Wood 2 only]a[/][Wood 1 only]b[/][Bronze only]c[/][Silver+](d)[/]);", 3), "x c);")

# page skeleton per level on a small document
mini = """> ### League introduction
> This is a **league based** challenge. For this challenge, leagues exist.
>
> [Wood 2 only] **This league:** Night falls.
>
> [Silver+] **This league:** Tides.
>
> ### Goal
> Score more.
>
> ### The reef
> A grid.
>
> ### Victory conditions
> - Have the highest score.
>
> ### Defeat conditions
> - Lower score.
>
> ### Game input
> **Initialization input**
> - Line 1: an integer `N`.
>
> **Input for one game turn**
> - Line 1: an integer `turn`.
>
> **Output for one game turn**
> - A single line.
>
> **Constraints**
> - 2 ≤ `playerCount` ≤ 4
>
> ### Technical details
> - Rows are sorted.
>
> ### League preview
> [Wood 2 only] Rocks next."""
p1, p4 = G.page(mini, 1), G.page(mini, 4)
for cls in ['class="statement_back"', 'statement-league-alert-content', "This league:</b> Night falls.", 'statement-section statement-goal',
            'icon-goal', 'statement-rules-content', '>The reef</h3>', 'statement-victory-conditions', 'statement-lose-conditions',
            'statement-expertrules', 'Technical Details', 'statement-protocol', 'Game Input/Output',
            '<div class="title">Initialization Input</div>', '<div class="title">Input for one game turn</div>',
            '<div class="title">Output for one game turn</div>', '<div class="title">Constraints</div>',
            'What is in store for me in the higher leagues?', '<li>Rocks next.</li>']:
    check("level1 has " + cls, cls in p1, True)
hidden_tail = "> ### Example\n> Text.\n> [Bronze+] Later.\n>\n> ### Game input\n> **Constraints**\n> - 2 ≤ `N`"
check("heading after a hidden last line", [t for t, n, b in G._sections(G.level_markdown(hidden_tail, 1))], ["Example", "Game input"])
check("level4 no preview", "What is in store" in p4, False)
check("level4 opener", "This league:</b> Tides." in p4, True)
check("no h2 for subsections", "<h2>The reef</h2>" in p1, False)

check("v5 preferred", G.statement_block("## Statement v4\n> old\n## Statement v5 (staged)\n> new\n# History\n"), "> new")
check("v4 fallback", G.statement_block("## Statement v4\n> old\n# History\n"), "> old")
check("statement pitch is neutral (V5.5)", bool(re.search(r"\byou(r)?\b", G.STATEMENT_PITCH, re.I)), False)
check("statement pitch text", G.STATEMENT_PITCH, "Players bend their lamps' beams with shared mirrors to hold the reef's crystals alone. 2 to 4 players.")
print("OK", "gen_statements tests")
