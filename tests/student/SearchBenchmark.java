import java.util.*;

/** Reproducible direct-method benchmark; compile against Java/source and run in one JVM. */
public final class SearchBenchmark {
    private static final long SEED = 20261108L;
    private static Map<String, Object> request(String task, String text, String pattern) {
        Map<String, Object> request = new HashMap<>();
        request.put("task", task); request.put("text", text); request.put("pattern", pattern);
        return request;
    }
    private static List<Integer> call(String task, String text, String pattern) {
        Map<String, Object> input = request(task, text, pattern);
        Map<String, Object> output;
        switch (task) {
            case "naive": output = Algorithms.naive(input); break;
            case "kmp": output = Algorithms.kmp(input); break;
            default: output = Algorithms.rk(input);
        }
        return (List<Integer>) output.get("positions");
    }
    private static String randomText(Random random, int length) {
        StringBuilder text = new StringBuilder(length);
        for (int i = 0; i < length; i++) text.append((char) ('a' + random.nextInt(26)));
        return text.toString();
    }
    private static void benchmark(String kind, int n, String text, String pattern) {
        String[] algorithms = {"naive", "kmp", "rk"};
        List<Integer> expected = call("naive", text, pattern);
        for (String algorithm : algorithms)
            if (!expected.equals(call(algorithm, text, pattern))) throw new AssertionError("wrong " + algorithm + " output");
        for (String algorithm : algorithms) {
            for (int warmup = 0; warmup < 10; warmup++) call(algorithm, text, pattern);
            double[] micros = new double[3];
            for (int run = 0; run < 3; run++) {
                long start = System.nanoTime();
                List<Integer> answer = call(algorithm, text, pattern);
                long elapsed = System.nanoTime() - start;
                if (!expected.equals(answer)) throw new AssertionError("output changed during timing");
                micros[run] = elapsed / 1000.0;
            }
            double median = Math.max(Math.min(micros[0], micros[1]), Math.min(Math.max(micros[0], micros[1]), micros[2]));
            System.out.printf(Locale.ROOT, "%s,%d,%d,%d,%s,%.3f,%.3f,%.3f,%.3f,true%n",
                    kind, n, pattern.length(), SEED, algorithm, micros[0], micros[1], micros[2], median);
        }
    }
    public static void main(String[] args) {
        System.out.println("case,n,m,seed,algorithm,run1_us,run2_us,run3_us,median_us,outputs_checked");
        Random random = new Random(SEED);
        for (int n : new int[]{300, 1000, 3000}) {
            int m = 64;
            String text = randomText(random, n);
            int at = random.nextInt(n - m + 1);
            benchmark("random", n, text, text.substring(at, at + m));
            String repetitiveText = "a".repeat(n);
            String repetitivePattern = "a".repeat(m - 2) + "bB";
            benchmark("repetitive-collision", n, repetitiveText, repetitivePattern);
        }
    }
}
