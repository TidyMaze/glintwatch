# Referee (CodinGame SDK 4.7.8, Java 8 bytecode)

- Build and test: `mvn test`
- Local viewer (4 bots): `mvn -q dependency:build-classpath -Dmdep.outputFile=cp.txt && timeout 600 java -cp "target/classes:target/test-classes:$(cat cp.txt)" Main [seed]`, then open http://localhost:8888/test.html

## Packaging (M6)
- Statements: `python3 -I tools/test_gen_statements.py && python3 -I tools/gen_statements.py` writes `config/level1-4/statement_en.html` from DESIGN.md "Statement v4" (league tags resolved per level).
- Bosses: `python3 -I tools/bundle_boss.py` writes `config/level2-4/Boss.java` (single-file `Player`: engine + bot); level1 is `Boss.py` (WAIT). Rerun after any engine or bot change; `BossFilesTest` fails if a bundle plays differently from its in-JVM bot.
- Export check: start `Main`, then `curl -s localhost:8888/services/export` must give `SUCCESS` with no report items.
