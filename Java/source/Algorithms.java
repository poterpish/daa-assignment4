import java.util.*;

/** Implementations for the eight JSON Lines commands in the assignment contract. */
public final class Algorithms {
    private Algorithms() {}

    private static String string(Map<String, Object> input, String key) {
        return (String) input.get(key);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Map<String, Object> input, String key) {
        return (List<Object>) input.get(key);
    }

    private static int intValue(Object value) {
        return ((Number) value).intValue();
    }

    private static long longValue(Object value) {
        return ((Number) value).longValue();
    }

    private static Map<String, Object> result(Object... fields) {
        Map<String, Object> answer = new LinkedHashMap<>();
        for (int i = 0; i < fields.length; i += 2) answer.put((String) fields[i], fields[i + 1]);
        return answer;
    }

    /** Prefix dynamic programming with a backtrack to one longest common subsequence. */
    static Map<String, Object> lcs(Map<String, Object> input) {
        String a = string(input, "a"), b = string(input, "b");
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) dp[i][j] = dp[i - 1][j - 1] + 1;
                else dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        StringBuilder reverse = new StringBuilder(dp[a.length()][b.length()]);
        int i = a.length(), j = b.length();
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) {
                reverse.append(a.charAt(i - 1)); i--; j--;
            } else if (dp[i - 1][j] >= dp[i][j - 1]) i--;
            else j--;
        }
        return result("length", dp[a.length()][b.length()], "sequence", reverse.reverse().toString());
    }

    /** Interval DP for adjacent merges; split points reconstruct a postorder plan. */
    static Map<String, Object> merge(Map<String, Object> input) {
        List<Object> values = list(input, "sizes");
        int n = values.size();
        if (n <= 1) return result("cost", 0L, "plan", new ArrayList<>());
        long[] prefix = new long[n + 1];
        for (int i = 0; i < n; i++) prefix[i + 1] = prefix[i] + longValue(values.get(i));
        long[][] dp = new long[n][n];
        int[][] split = new int[n][n];
        for (int length = 2; length <= n; length++) {
            for (int left = 0; left + length <= n; left++) {
                int right = left + length - 1;
                long best = Long.MAX_VALUE;
                for (int middle = left; middle < right; middle++) {
                    long candidate = dp[left][middle] + dp[middle + 1][right]
                            + prefix[right + 1] - prefix[left];
                    if (candidate < best) { best = candidate; split[left][right] = middle; }
                }
                dp[left][right] = best;
            }
        }
        List<Object> plan = new ArrayList<>(n - 1);
        appendPlan(0, n - 1, split, plan);
        return result("cost", dp[0][n - 1], "plan", plan);
    }

    private static void appendPlan(int left, int right, int[][] split, List<Object> plan) {
        if (left == right) return;
        int middle = split[left][right];
        appendPlan(left, middle, split, plan);
        appendPlan(middle + 1, right, split, plan);
        plan.add(Arrays.asList(left, middle, right));
    }

    /** Iterative take/skip tree DP and reconstruction (safe for a 100,000-node chain). */
    static Map<String, Object> tree(Map<String, Object> input) {
        int n = intValue(input.get("n"));
        List<Object> weightInput = list(input, "weights"), edges = list(input, "edges");
        if (n == 0) return result("weight", 0L, "selected", new ArrayList<>());
        long[] weights = new long[n];
        for (int i = 0; i < n; i++) weights[i] = longValue(weightInput.get(i));
        int[] degree = new int[n];
        for (Object edgeObject : edges) {
            List<Object> edge = (List<Object>) edgeObject;
            degree[intValue(edge.get(0))]++; degree[intValue(edge.get(1))]++;
        }
        int[][] graph = new int[n][];
        for (int i = 0; i < n; i++) graph[i] = new int[degree[i]];
        Arrays.fill(degree, 0);
        for (Object edgeObject : edges) {
            List<Object> edge = (List<Object>) edgeObject;
            int u = intValue(edge.get(0)), v = intValue(edge.get(1));
            graph[u][degree[u]++] = v; graph[v][degree[v]++] = u;
        }

        int[] parent = new int[n], order = new int[n], stack = new int[n];
        Arrays.fill(parent, -2);
        parent[0] = -1;
        int top = 0, count = 0;
        stack[top++] = 0;
        while (top > 0) {
            int u = stack[--top];
            order[count++] = u;
            for (int v : graph[u]) if (parent[v] == -2) { parent[v] = u; stack[top++] = v; }
        }
        long[] take = new long[n], skip = new long[n];
        for (int i = n - 1; i >= 0; i--) {
            int u = order[i];
            take[u] = weights[u];
            for (int v : graph[u]) if (parent[v] == u) {
                take[u] += skip[v];
                skip[u] += Math.max(take[v], skip[v]);
            }
        }
        List<Integer> selected = new ArrayList<>();
        int[] nodes = new int[n]; boolean[] chosen = new boolean[n];
        top = 0; nodes[top] = 0; chosen[top++] = take[0] > skip[0];
        while (top > 0) {
            int u = nodes[--top]; boolean parentChosen = chosen[top];
            if (parentChosen) selected.add(u);
            for (int v : graph[u]) if (parent[v] == u) {
                nodes[top] = v;
                chosen[top++] = !parentChosen && take[v] > skip[v];
            }
        }
        return result("weight", Math.max(take[0], skip[0]), "selected", selected);
    }

    /** Direct character-by-character search, including every valid start. */
    static Map<String, Object> naive(Map<String, Object> input) {
        return result("positions", naivePositions(string(input, "text"), string(input, "pattern")));
    }

    private static List<Integer> naivePositions(String text, String pattern) {
        int n = text.length(), m = pattern.length();
        List<Integer> positions = new ArrayList<>();
        if (m == 0) { for (int i = 0; i <= n; i++) positions.add(i); return positions; }
        for (int start = 0; start + m <= n; start++) {
            int j = 0;
            while (j < m && text.charAt(start + j) == pattern.charAt(j)) j++;
            if (j == m) positions.add(start);
        }
        return positions;
    }

    /** Prefix-function KMP; fallback after a match keeps overlapping occurrences. */
    static Map<String, Object> kmp(Map<String, Object> input) {
        String text = string(input, "text"), pattern = string(input, "pattern");
        int n = text.length(), m = pattern.length();
        List<Integer> positions = new ArrayList<>();
        if (m == 0) { for (int i = 0; i <= n; i++) positions.add(i); return result("positions", positions); }
        int[] pi = new int[m];
        for (int i = 1, j = 0; i < m; i++) {
            while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) j = pi[j - 1];
            if (pattern.charAt(i) == pattern.charAt(j)) j++;
            pi[i] = j;
        }
        for (int i = 0, j = 0; i < n; i++) {
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) j = pi[j - 1];
            if (text.charAt(i) == pattern.charAt(j)) j++;
            if (j == m) { positions.add(i - m + 1); j = pi[j - 1]; }
        }
        return result("positions", positions);
    }

    /** Rabin-Karp with base 31, modulus 101 and exact verification of hash candidates. */
    static Map<String, Object> rk(Map<String, Object> input) {
        String text = string(input, "text"), pattern = string(input, "pattern");
        int n = text.length(), m = pattern.length();
        List<Integer> positions = new ArrayList<>();
        if (m == 0) { for (int i = 0; i <= n; i++) positions.add(i); return result("positions", positions); }
        if (m > n) return result("positions", positions);
        final int base = 31, modulus = 101;
        int highPower = 1;
        for (int i = 1; i < m; i++) highPower = highPower * base % modulus;
        int patternHash = 0, windowHash = 0;
        for (int i = 0; i < m; i++) {
            patternHash = (patternHash * base + pattern.charAt(i)) % modulus;
            windowHash = (windowHash * base + text.charAt(i)) % modulus;
        }
        for (int start = 0; start + m <= n; start++) {
            if (patternHash == windowHash && matchesAt(text, pattern, start)) positions.add(start);
            if (start + m < n) {
                windowHash = (windowHash - text.charAt(start) * highPower % modulus + modulus) % modulus;
                windowHash = (windowHash * base + text.charAt(start + m)) % modulus;
            }
        }
        return result("positions", positions);
    }

    private static boolean matchesAt(String text, String pattern, int start) {
        for (int i = 0; i < pattern.length(); i++) if (text.charAt(start + i) != pattern.charAt(i)) return false;
        return true;
    }

    /** Prefix-doubling suffix array with two stable counting-sort passes, plus Kasai LCP. */
    static Map<String, Object> suffix(Map<String, Object> input) {
        String text = string(input, "text");
        int n = text.length();
        List<Integer> saOutput = new ArrayList<>(n), lcpOutput = new ArrayList<>(n);
        if (n == 0) return result("sa", saOutput, "lcp", lcpOutput, "repeated", "");
        int[] sa = new int[n], rank = new int[n], nextRank = new int[n], temp = new int[n];
        int[] counts = new int[Math.max(n + 2, 96)];
        for (int i = 0; i < n; i++) { sa[i] = i; rank[i] = text.charAt(i) - 32; }
        int classes = 95;
        for (int width = 1; width < n; width <<= 1) {
            countingSort(sa, temp, rank, width, classes, counts);
            countingSort(temp, sa, rank, 0, classes, counts);
            int newClasses = 1;
            nextRank[sa[0]] = 0;
            for (int i = 1; i < n; i++) {
                int previous = sa[i - 1], current = sa[i];
                int previousSecond = previous + width < n ? rank[previous + width] : -1;
                int currentSecond = current + width < n ? rank[current + width] : -1;
                if (rank[previous] != rank[current] || previousSecond != currentSecond) newClasses++;
                nextRank[current] = newClasses - 1;
            }
            int[] swap = rank; rank = nextRank; nextRank = swap; classes = newClasses;
            if (classes == n) break;
        }
        int[] inverse = new int[n];
        for (int i = 0; i < n; i++) { saOutput.add(sa[i]); inverse[sa[i]] = i; }
        int[] lcp = new int[n]; int common = 0, bestLength = 0, bestStart = 0;
        for (int start = 0; start < n; start++) {
            int order = inverse[start];
            if (order == 0) { common = 0; continue; }
            int other = sa[order - 1];
            while (start + common < n && other + common < n
                    && text.charAt(start + common) == text.charAt(other + common)) common++;
            lcp[order] = common;
            if (common > bestLength) { bestLength = common; bestStart = start; }
            if (common > 0) common--;
        }
        for (int value : lcp) lcpOutput.add(value);
        String repeated = bestLength == 0 ? "" : text.substring(bestStart, bestStart + bestLength);
        return result("sa", saOutput, "lcp", lcpOutput, "repeated", repeated);
    }

    private static void countingSort(int[] source, int[] target, int[] rank, int offset,
                                     int classes, int[] counts) {
        int keyCount = classes + 1;
        Arrays.fill(counts, 0, keyCount, 0);
        int n = source.length;
        for (int index : source) {
            int position = index + offset;
            int key = position < n ? rank[position] + 1 : 0;
            counts[key]++;
        }
        int total = 0;
        for (int key = 0; key < keyCount; key++) {
            int amount = counts[key]; counts[key] = total; total += amount;
        }
        for (int index : source) {
            int position = index + offset;
            int key = position < n ? rank[position] + 1 : 0;
            target[counts[key]++] = index;
        }
    }

    /** Aho-Corasick with a dense ASCII transition table and reverse failure aggregation. */
    static Map<String, Object> aho(Map<String, Object> input) {
        List<Object> patternInput = list(input, "patterns"), textInput = list(input, "texts");
        int patternCount = patternInput.size();
        List<String> patterns = new ArrayList<>(patternCount), texts = new ArrayList<>(textInput.size());
        int totalLength = 0;
        for (Object value : patternInput) { String pattern = (String) value; patterns.add(pattern); totalLength += pattern.length(); }
        for (Object value : textInput) texts.add((String) value);
        int capacity = totalLength + 1;
        int[][] next = new int[capacity][95];
        int[] failure = new int[capacity], terminal = new int[patternCount];
        int states = 1;
        for (int p = 0; p < patternCount; p++) {
            int state = 0; String pattern = patterns.get(p);
            for (int i = 0; i < pattern.length(); i++) {
                int c = pattern.charAt(i) - 32;
                if (next[state][c] == 0) next[state][c] = states++;
                state = next[state][c];
            }
            terminal[p] = state;
        }
        int[] order = new int[states], queue = new int[states];
        int head = 0, tail = 0, orderSize = 0;
        order[orderSize++] = 0;
        for (int c = 0; c < 95; c++) {
            int child = next[0][c];
            if (child != 0) { queue[tail++] = child; order[orderSize++] = child; }
        }
        while (head < tail) {
            int state = queue[head++];
            for (int c = 0; c < 95; c++) {
                int child = next[state][c];
                if (child != 0) {
                    failure[child] = next[failure[state]][c];
                    queue[tail++] = child; order[orderSize++] = child;
                } else next[state][c] = next[failure[state]][c];
            }
        }
        List<Object> rows = new ArrayList<>(texts.size());
        for (String text : texts) {
            long[] visits = new long[states]; int state = 0;
            for (int i = 0; i < text.length(); i++) {
                state = next[state][text.charAt(i) - 32];
                visits[state]++;
            }
            for (int i = orderSize - 1; i > 0; i--) {
                int node = order[i]; visits[failure[node]] += visits[node];
            }
            List<Long> row = new ArrayList<>(patternCount);
            for (int node : terminal) row.add(visits[node]);
            rows.add(row);
        }
        return result("counts", rows);
    }
}
