import java.util.*;

/** Deterministic boundary, cross-check and scale checks for the Java submission. */
public final class StudentChecks {
    private static Map<String, Object> request(String task, Object... values) {
        Map<String, Object> request = new LinkedHashMap<>(); request.put("task", task);
        for (int i = 0; i < values.length; i += 2) request.put((String) values[i], values[i + 1]);
        return request;
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static List<Integer> positions(Map<String, Object> answer) {
        return (List<Integer>) answer.get("positions");
    }
    private static boolean subsequence(String small, String large) {
        int at = 0;
        for (int i = 0; i < large.length() && at < small.length(); i++)
            if (small.charAt(at) == large.charAt(i)) at++;
        return at == small.length();
    }
    private static List<Integer> oracle(String text, String pattern) {
        List<Integer> answer = new ArrayList<>();
        if (pattern.isEmpty()) { for (int i = 0; i <= text.length(); i++) answer.add(i); return answer; }
        for (int i = 0; i + pattern.length() <= text.length(); i++)
            if (text.regionMatches(i, pattern, 0, pattern.length())) answer.add(i);
        return answer;
    }
    public static void main(String[] args) {
        Map<String, Object> lcs = Algorithms.lcs(request("lcs", "a", "", "b", "abc"));
        require(lcs.get("length").equals(0) && lcs.get("sequence").equals(""), "empty LCS");
        lcs = Algorithms.lcs(request("lcs", "a", "axbyc", "b", "abc"));
        require(lcs.get("length").equals(3) && subsequence((String) lcs.get("sequence"), "axbyc")
                && subsequence((String) lcs.get("sequence"), "abc"), "LCS witness");

        Map<String, Object> merge = Algorithms.merge(request("merge", "sizes", Arrays.asList(2L, 3L, 4L)));
        require(merge.get("cost").equals(14L) && ((List<?>) merge.get("plan")).size() == 2, "merge optimum/plan");
        require(Algorithms.merge(request("merge", "sizes", Collections.emptyList())).get("cost").equals(0L), "empty merge");

        Map<String, Object> tree = Algorithms.tree(request("tree", "n", 3L, "weights", Arrays.asList(4L, 9L, 6L),
                "edges", Arrays.asList(Arrays.asList(0L, 1L), Arrays.asList(1L, 2L))));
        require(tree.get("weight").equals(10L) && ((List<?>) tree.get("selected")).size() == 2, "tree witness");
        int chainSize = 100_000;
        List<Object> chainEdges = new ArrayList<>(chainSize - 1), chainWeights = new ArrayList<>(chainSize);
        for (int i = 0; i < chainSize; i++) { chainWeights.add(1L); if (i > 0) chainEdges.add(Arrays.asList((long) i - 1, (long) i)); }
        tree = Algorithms.tree(request("tree", "n", (long) chainSize, "weights", chainWeights, "edges", chainEdges));
        require(tree.get("weight").equals(50_000L) && ((List<?>) tree.get("selected")).size() == 50_000,
                "deep tree iterative reconstruction");
        List<Object> mergeSizes = new ArrayList<>(200);
        for (int i = 0; i < 200; i++) mergeSizes.add(1L);
        merge = Algorithms.merge(request("merge", "sizes", mergeSizes));
        require(((List<?>) merge.get("plan")).size() == 199, "200-block merge scale");
        String longSequence = "ab".repeat(500);
        lcs = Algorithms.lcs(request("lcs", "a", longSequence, "b", "ba".repeat(500)));
        require(lcs.get("length").equals(999), "maximum-size LCS");

        require(positions(Algorithms.rk(request("rk", "text", "abdj", "pattern", "ab"))).equals(Arrays.asList(0)), "RK collision verification");
        Random random = new Random(20261108L);
        for (int trial = 0; trial < 300; trial++) {
            int n = random.nextInt(45), m = random.nextInt(18);
            StringBuilder text = new StringBuilder(), pattern = new StringBuilder();
            for (int i = 0; i < n; i++) text.append((char) (32 + random.nextInt(95)));
            for (int i = 0; i < m; i++) pattern.append((char) (32 + random.nextInt(95)));
            List<Integer> expected = oracle(text.toString(), pattern.toString());
            require(positions(Algorithms.naive(request("naive", "text", text.toString(), "pattern", pattern.toString()))).equals(expected), "naive oracle");
            require(positions(Algorithms.kmp(request("kmp", "text", text.toString(), "pattern", pattern.toString()))).equals(expected), "KMP oracle");
            require(positions(Algorithms.rk(request("rk", "text", text.toString(), "pattern", pattern.toString()))).equals(expected), "RK oracle");
        }
        String longText = "a".repeat(200_000), longPattern = "a".repeat(99_999) + "b";
        require(positions(Algorithms.kmp(request("kmp", "text", longText, "pattern", longPattern))).isEmpty(),
                "maximum-size KMP");
        longText = "a".repeat(10_000); longPattern = "a".repeat(1_998) + "bB";
        require(positions(Algorithms.rk(request("rk", "text", longText, "pattern", longPattern))).isEmpty(),
                "maximum-product collision-heavy RK");

        Map<String, Object> suffix = Algorithms.suffix(request("suffix", "text", "banana"));
        require(suffix.get("sa").equals(Arrays.asList(5, 3, 1, 0, 4, 2))
                && suffix.get("lcp").equals(Arrays.asList(0, 1, 3, 0, 0, 2))
                && suffix.get("repeated").equals("ana"), "suffix example");
        suffix = Algorithms.suffix(request("suffix", "text", "a".repeat(50_000)));
        require(((List<?>) suffix.get("sa")).size() == 50_000
                && ((List<?>) suffix.get("lcp")).get(49_999).equals(49_999)
                && ((String) suffix.get("repeated")).length() == 49_999, "maximum-size suffix array");
        for (int trial = 0; trial < 200; trial++) {
            int n = random.nextInt(25); StringBuilder s = new StringBuilder();
            for (int i = 0; i < n; i++) s.append((char) ('a' + random.nextInt(4)));
            suffix = Algorithms.suffix(request("suffix", "text", s.toString()));
            List<Integer> sa = (List<Integer>) suffix.get("sa"), lcp = (List<Integer>) suffix.get("lcp");
            List<Integer> expectedSa = new ArrayList<>(); for (int i = 0; i < n; i++) expectedSa.add(i);
            expectedSa.sort((x, y) -> s.substring(x).compareTo(s.substring(y)));
            require(sa.equals(expectedSa), "suffix ordering oracle");
            for (int i = 0; i < n; i++) {
                int length = 0;
                if (i > 0) while (sa.get(i) + length < n && sa.get(i - 1) + length < n
                        && s.charAt(sa.get(i) + length) == s.charAt(sa.get(i - 1) + length)) length++;
                require(lcp.get(i) == length, "Kasai LCP oracle");
            }
            int longest = 0;
            for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) {
                int length = 0; while (i + length < n && j + length < n && s.charAt(i + length) == s.charAt(j + length)) length++;
                longest = Math.max(longest, length);
            }
            require(((String) suffix.get("repeated")).length() == longest, "longest repeated substring oracle");
        }

        Map<String, Object> aho = Algorithms.aho(request("aho", "patterns", Arrays.asList("he", "she", "he"),
                "texts", Arrays.asList("sheshe", "", "sheshe")));
        require(aho.get("counts").equals(Arrays.asList(Arrays.asList(2L, 2L, 2L), Arrays.asList(0L, 0L, 0L),
                Arrays.asList(2L, 2L, 2L))), "Aho suffix/duplicate/reset");
        aho = Algorithms.aho(request("aho", "patterns", Arrays.asList("a".repeat(100_000)),
                "texts", Arrays.asList("a".repeat(200_000))));
        require(((List<?>) ((List<?>) aho.get("counts")).get(0)).get(0).equals(100_001L),
                "maximum-size Aho-Corasick");
        System.out.println("PASS Java student checks (published scale limits, 100,000-node tree and randomized oracles)");
    }
}
