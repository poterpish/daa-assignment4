# Assignment 4 — analysis

## Environment and commands

- Selected language: Java 17. The implementation uses only the Java standard library and the supplied JSON adapter.
- Verification environment: Windows 11 (build 26200), 12th Gen Intel Core i5-12500H, OpenJDK 25.0.1. Compilation used `--release 17`. The sandbox denied the Windows memory/CIM query, so installed RAM could not be recorded.
- Compile from the repository root: `javac --release 17 -encoding UTF-8 -d Java/build Java/source/Algorithms.java Java/source/Json.java Java/source/Main.java`
- Run JSON Lines: `java -Xmx384m -cp Java/build Main`
- Compile and run student checks: `javac --release 17 -encoding UTF-8 -cp Java/build -d tests/student tests/student/StudentChecks.java tests/student/SearchBenchmark.java`, then `java -Xmx384m -cp "Java/build;tests/student" StudentChecks` (Windows class-path separator).
- Reproduce search timings: `java -Xmx384m -cp "Java/build;tests/student" SearchBenchmark`. The benchmark performs ten warm-up calls per algorithm and case, checks all three outputs against naive, then reports three measured runs. Compilation and JSON parsing/serialization are outside the timed scope; the algorithm call and result-list creation are inside it.
- `python tools/run.py --check` could not be invoked in this environment because its `python.exe` WindowsApps shim is blocked. The Java sources and own checks were compiled and run directly with the installed JDK.

## Algorithm explanations

### LCS

`dp[i][j]` is the LCS length of the first `i` characters of `a` and first `j` characters of `b`. Row 0 and column 0 are zero. Equal final characters extend the diagonal optimum by one; for unequal characters, an optimum must omit at least one of the two final characters, so the larger of `dp[i-1][j]` and `dp[i][j-1]` is sufficient. Increasing `i` and `j` makes both dependencies available. Backtracking follows the equality diagonal or a neighbor with the same maximum value; reversing the collected characters gives a subsequence of each input with the DP optimum length. Ties may choose either neighbor. Time and auxiliary memory are O((|a|+1)(|b|+1)); output length is at most min(|a|,|b|). Empty strings work through the zero row/column. The empty-input check detects incorrectly indexing the first character or omitting the base cases.

### Ordered adjacent-block merge

`dp[l][r]` is the minimum cost to merge the consecutive original blocks from `l` through `r`. A prefix sum gives the size of that interval. A singleton has cost zero; for every larger interval, try each final split `k` and add the two optimal child costs plus the interval size (the cost of its last merge). The interval-length loop ensures both smaller intervals are complete before use. Store the minimizing split and recursively emit its left plan, right plan, and then `[l,k,r]`, which is chronological postorder and always merges adjacent intervals. Induction over interval length proves each stored cost optimal because the last merge has exactly one split and each side must itself be optimal. There are O(n²) states and O(n) splits per state: O(n³) time, O(n²) memory, and O(n) plan output. Prefix sums and costs use `long`. The `[2,3,4]` test detects confusion with unrestricted Huffman-style merging; the three-billion-size case detects 32-bit cost overflow.

### Tree independent set

Root the tree at vertex 0. `take[u]` is the best weight in `u`'s rooted subtree when `u` is selected; `skip[u]` is the best when it is not. Thus `take[u]=weight[u]+sum(skip[child])` and `skip[u]=sum(max(take[child],skip[child]))`. An explicit stack builds parent and preorder arrays, and reversing that order processes every child before its parent. Reconstruction follows a second explicit stack: a chosen parent forces every child to skip; otherwise choose a child only when its take value is strictly better. A tie may skip, and the empty set is retained by zero-valued skip states. The recurrence partitions the independent sets according to whether each node is present, proving both values optimal. Adjacency lists, DP, parent/order, and reconstruction are O(n) time and memory; there is no recursion-depth risk. Weights and sums use `long`. The 100,000-node chain check detects recursive stack overflow and verifies the selected witness size.

### Naive exact search

For each start `0..n-m`, compare `pattern[j]` to `text[start+j]` until a mismatch or all `m` characters match. Advancing the start by one includes overlaps and the final legal start. For `m=0`, return boundaries `0..n`; for `m>n`, the range is empty. Each window comparison is exact, so every reported start is valid and every valid start is examined. Time is O(nm+n+1), including empty-pattern output; auxiliary memory is O(1) beyond O(z) result storage. The `aaaa`/`aa` case checks that overlaps are returned, and the empty-pattern case checks the final boundary.

### KMP

`pi[i]` is the length of the longest proper prefix of the pattern that is also a suffix ending at `i`. Build it left-to-right, falling back to `pi[j-1]` after a mismatch; each fallback preserves the longest still-possible border. During search, text index `i` only moves forward, while the matched-prefix length `j` falls back through the same border chain. On a complete match, recording `i-m+1` and setting `j=pi[j-1]` preserves overlaps. The prefix table is ready before search. This invariant accounts for every possible suffix of the consumed text that can begin a future match, proving no candidate is lost. Empty patterns are handled before indexing. Time is O(n+m+z+1); auxiliary memory O(m+1), plus O(z+1) result storage. The `abababa`/`aba` case detects resetting `j` to zero and losing overlapping occurrences.

### Rabin–Karp

The hash is the polynomial over ASCII codes with base 31 and modulus 101, with the leftmost character at the highest power. Compute the pattern and first-window hashes, then remove the outgoing code times `31^(m-1)`, multiply by 31, and add the incoming code; normalize modulo 101. Equal hashes are only candidates, so compare all `m` characters before reporting. Handle an empty pattern and `m>n` before constructing a window. Correctness follows because every true match has the same hash and candidate verification rejects collisions. With `c` equal-hash windows, time is O(n+m+c·m); worst case is O(nm+n+m+1). Auxiliary memory is O(1), plus O(z) result storage. `ab` and `dj` collide under this configuration, so the explicit collision case catches false positives. The modulus is intentionally small and does not describe every production hash.

### Suffix array and Kasai LCP

The suffix array starts with ranks for single characters. At doubling width `w`, a suffix is ordered by `(rank[i], rank[i+w])`, using a missing second half below every real rank. Two stable counting-sort passes sort the rank pair without constructing suffix strings; equal pairs receive equal next-round ranks. Width doubles until all suffixes have unique ranks. There are O(log n) rounds, each with O(n+classes)=O(n) counting work, so construction is O(n log n) time and O(n) auxiliary memory. Output contains only starts `0..n-1`.

Kasai builds the inverse suffix order. For each start `i`, compare its suffix with its predecessor in suffix order. If the common prefix for the previous text position was `h`, the next comparison starts at `max(h-1,0)`; removing one leading character can reduce the LCP by at most one. Each text character advances the comparison frontier only O(1) amortized times, giving O(n) time and O(n) memory. `lcp[i]` is the common prefix of `sa[i-1]` and `sa[i]`; the longest repeated substring is represented by a maximum adjacent LCP, since any two suffixes sharing a prefix have all intervening suffixes sharing it too. Empty and one-character inputs return empty repeats. `banana`, randomized small strings, and all-equal strings check ordering, LCP, overlaps, and the absence of an output sentinel.

### Aho–Corasick

Trie states represent dictionary prefixes. A breadth-first construction assigns each state's failure link to the longest proper suffix that is also a trie prefix and fills missing transitions from the failure state's transitions. When scanning one text, record a visit at the reached state for every character. Process states in reverse BFS order and add each visit count to its failure state. This makes a terminal state's final visit count include all suffix-pattern matches without enumerating every individual output at every position. Each original dictionary position stores its terminal state, so duplicate patterns share work but receive duplicate equal counts. Each text gets a fresh visit array and root state. Construction is O(L+95S) time and O(95S+p) memory for total pattern length L, S trie states, p dictionary entries, and the fixed printable-ASCII alphabet. Each text costs O(|text|+S+p) including count output; all output rows cost O(qp) for q texts. The duplicate/suffix-pattern and repeated-text case catches missing failure aggregation or state leakage; empty dictionaries retain one empty row per text.

## Own tests

`tests/student/StudentChecks.java` runs deterministic boundary cases, checks the Rabin–Karp collision, checks all three searches against a direct oracle on 300 seeded random inputs, checks suffix order/LCP/repeat length against brute force on 200 seeded strings, and exercises the published maximum sizes for LCS, merge, KMP, RK, suffix array, Aho–Corasick and the 100,000-vertex tree chain.

|Command|Input or fixture|Expected result|Actual result / log|Defect checked|
|---|---|---|---|---|
|`javac --release 17 ... Java/source/*.java` (compiled as the three explicit source files listed above)|Java source|Successful compile|PASS, OpenJDK 25.0.1, `--release 17`|Build and Java 17 API compatibility|
|`java -Xmx384m -cp "Java/build;tests/student" StudentChecks`|Deterministic assertions, published scale limits and seeded generated cases|All assertions pass|`PASS Java student checks (published scale limits, 100,000-node tree and randomized oracles)`|Boundaries, witnesses, collision, mixed independent calls, scale and brute-force cross-checks|
|`java -Xmx384m -cp Java/build Main`|All 44 requests in `tests/public.json` in one process|44 valid JSON result lines|PASS, 44/44 responses parsed; three witness outputs differ from sample witnesses only by permitted optimal ties (two merge plans and the tied tree choice)|JSON Lines transport, schemas, public examples and request-state reset|
|`java -Xmx384m -cp "Java/build;tests/student" SearchBenchmark`|Three sizes, random and repetitive collision-heavy inputs|Same exact positions from naive, KMP and RK before and during timing|PASS; each output row reports `outputs_checked=true`|Search correctness, collision-sensitive workload and reproducible timing|
|`python tools/run.py --check`|44 supplied public cases and repeated mixed batch|All public checks pass|Not run: Python launcher could not start because the environment blocks its WindowsApps `python.exe` shim|The supplied Python launcher itself remains available for a normal Python-enabled environment|

## Search comparison

The benchmark uses Java 25.0.1, seed `20261108`, `n` = 300, 1,000, 3,000 and `m` = 64. Random text and pattern use lowercase ASCII; each random pattern is a substring of its generated text. The repetitive text is `a` repeated `n` times and the pattern is `a` repeated 62 times followed by `bB`. Its hash equals the hash of an all-`a` window: the final pair contributes the same polynomial value because `31*(b-a)+(B-a)=31-31=0`. Thus every repetitive window is an RK candidate and character verification scans 63 characters before rejecting it. Outputs were compared with naive before measurement and after every timed run. Times are microseconds for the algorithm call plus result-list creation; input generation, compilation, JSON parsing/serialization, and process startup are excluded. Each case/algorithm had ten warm-up calls; the three measured runs are raw, and median is reported. The generator and timing code are in `tests/student/SearchBenchmark.java`.

|Case|n|m|Seed/generator|Algorithm|Run 1 (µs)|Run 2 (µs)|Run 3 (µs)|Median (µs)|Correct outputs checked|
|---|---:|---:|---|---|---:|---:|---:|---:|---|
|Random|300|64|20261108, lowercase random text; pattern copied from text|naive|25.600|23.200|21.400|23.200|Yes|
|Random|300|64|same|kmp|30.700|42.100|50.000|42.100|Yes|
|Random|300|64|same|rk|49.600|56.900|44.300|49.600|Yes|
|Repetitive collision|300|64|20261108, `a` text; `a`×62+`bB` pattern|naive|273.200|247.600|352.000|273.200|Yes|
|Repetitive collision|300|64|same|kmp|34.700|45.800|75.600|45.800|Yes|
|Repetitive collision|300|64|same|rk|596.100|618.700|618.800|618.700|Yes|
|Random|1,000|64|20261108, lowercase random text; pattern copied from text|naive|40.800|22.900|25.400|25.400|Yes|
|Random|1,000|64|same|kmp|53.900|52.000|49.300|52.000|Yes|
|Random|1,000|64|same|rk|88.200|283.600|82.900|88.200|Yes|
|Repetitive collision|1,000|64|20261108, `a` text; `a`×62+`bB` pattern|naive|492.500|371.000|294.900|371.000|Yes|
|Repetitive collision|1,000|64|same|kmp|116.400|142.300|134.000|134.000|Yes|
|Repetitive collision|1,000|64|same|rk|280.800|264.600|348.800|280.800|Yes|
|Random|3,000|64|20261108, lowercase random text; pattern copied from text|naive|143.400|112.100|149.800|143.400|Yes|
|Random|3,000|64|same|kmp|69.300|91.000|89.500|89.500|Yes|
|Random|3,000|64|same|rk|243.200|251.900|260.600|251.900|Yes|
|Repetitive collision|3,000|64|20261108, `a` text; `a`×62+`bB` pattern|naive|349.100|351.800|352.700|351.800|Yes|
|Repetitive collision|3,000|64|same|kmp|288.300|272.600|288.500|288.300|Yes|
|Repetitive collision|3,000|64|same|rk|902.700|816.600|652.500|816.600|Yes|

On the random inputs, mismatches usually stop naive comparisons early, while KMP and RK do more fixed bookkeeping per character in these small measurements. On the repetitive collision inputs, RK verifies many equal-hash windows and becomes slower; KMP avoids rescanning the long `a` prefixes. The timing noise and workload-specific pattern distribution mean these values illustrate the chosen cases only; the complexity bounds come from the algorithms, not from these measurements.

## Sources and AI assistance

- Sources: the assignment specification and contracts in `docs/Assignment_4_EN.md` and `docs/guide/CONTRACTS.md`; the provided `Java/source/Json.java` adapter; Java standard library documentation embodied by the JDK 17 API.
- AI assistance: OpenAI Codex drafted the Java algorithms, student checks, benchmark harness, and this analysis. The student should review each algorithm and be prepared to explain it independently during the defence.
- Verification performed here: Java 17-targeted compilation on OpenJDK 25.0.1; the deterministic/student checks and seeded oracle comparisons passed; benchmark output equality was checked before timing and on each measured call. The official Python public-check launcher could not be run in this sandbox because the WindowsApps Python executable was blocked.
- Final commit URL: add the URL containing the full commit SHA to Moodle after pushing this commit to the student's repository.
