# Java student checks and search benchmark

Compile the application from the repository root:

```powershell
javac --release 17 -encoding UTF-8 -d Java/build Java/source/Algorithms.java Java/source/Json.java Java/source/Main.java
javac --release 17 -encoding UTF-8 -cp Java/build -d tests/student tests/student/StudentChecks.java tests/student/SearchBenchmark.java
```

Run the deterministic checks and reproducible benchmark:

```powershell
java -Xmx384m -cp "Java/build;tests/student" StudentChecks
java -Xmx384m -cp "Java/build;tests/student" SearchBenchmark
```

The checks include published maximum-size LCS, merge, KMP, RK, suffix array, Aho–Corasick, and tree inputs; 300 seeded search-oracle cases; 200 suffix-array/LCP brute-force cases; and a Rabin–Karp collision. Benchmark timing details and recorded results are in the repository's `ANALYSIS.md`.
