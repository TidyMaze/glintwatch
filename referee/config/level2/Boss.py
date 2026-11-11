import sys

# Glintwatch league level 2 boss placeholder (DESIGN F9): reads the whole input of each turn, then waits.
def read():
    line = sys.stdin.readline()
    if not line:
        sys.exit(0)
    return line.strip()

players = int(read().split()[0])
n = int(read())
for _ in range(players + n):
    read()
for _ in range(int(read())):
    read()
while True:
    read()
    for _ in range(players):
        read()
    for _ in range(int(read())):
        read()
    for _ in range(int(read())):
        read()
    print("WAIT", flush=True)
