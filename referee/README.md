# Referee (CodinGame SDK 4.7.8, Java 8 bytecode)

- Build and test: `mvn test`
- Local viewer (4 bots): `mvn -q dependency:build-classpath -Dmdep.outputFile=cp.txt && timeout 600 java -cp "target/classes:target/test-classes:$(cat cp.txt)" Main [seed]`, then open http://localhost:8888/test.html
