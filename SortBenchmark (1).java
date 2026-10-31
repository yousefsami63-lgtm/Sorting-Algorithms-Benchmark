import java.util.*;

public class SortBenchmark {
    static final int N = 100_000;

    // ---------- Selection Sort ----------
    static void selectionSort(int[] a) {
        int n = a.length;
        for (int i = 0; i < n - 1; i++) {
            int min = i;
            for (int j = i + 1; j < n; j++) if (a[j] < a[min]) min = j;
            int t = a[min]; a[min] = a[i]; a[i] = t;
        }
    }

    // ---------- Insertion Sort ----------
    static void insertionSort(int[] a) {
        for (int i = 1; i < a.length; i++) {
            int key = a[i], j = i - 1;
            while (j >= 0 && a[j] > key) { a[j + 1] = a[j]; j--; }
            a[j + 1] = key;
        }
    }

    // ---------- Bubble Sort (basic version, no early exit) ----------
    static void bubbleSort(int[] a) {
        int n = a.length;
        for (int i = 0; i < n - 1; i++)
            for (int j = 0; j < n - 1 - i; j++)
                if (a[j] > a[j + 1]) { int t = a[j]; a[j] = a[j + 1]; a[j + 1] = t; }
    }

    // ---------- Merge Sort ----------
    static void mergeSort(int[] a, int l, int r) {
        if (l >= r) return;
        int m = (l + r) >>> 1;
        mergeSort(a, l, m);
        mergeSort(a, m + 1, r);
        merge(a, l, m, r);
    }
    static void merge(int[] a, int l, int m, int r) {
        int[] L = Arrays.copyOfRange(a, l, m + 1);
        int[] R = Arrays.copyOfRange(a, m + 1, r + 1);
        int i = 0, j = 0, k = l;
        while (i < L.length && j < R.length) a[k++] = (L[i] <= R[j]) ? L[i++] : R[j++];
        while (i < L.length) a[k++] = L[i++];
        while (j < R.length) a[k++] = R[j++];
    }

    // ---------- Quick Sort (last element as pivot, Lomuto) ----------
    static void quickSort(int[] a, int lo, int hi) {
        if (lo < hi) {
            int p = partition(a, lo, hi);
            quickSort(a, lo, p - 1);
            quickSort(a, p + 1, hi);
        }
    }
    static int partition(int[] a, int lo, int hi) {
        int pivot = a[hi], i = lo - 1;
        for (int j = lo; j < hi; j++)
            if (a[j] <= pivot) { i++; int t = a[i]; a[i] = a[j]; a[j] = t; }
        int t = a[i + 1]; a[i + 1] = a[hi]; a[hi] = t;
        return i + 1;
    }

    // ---------- Data generators ----------
    static int[] random(Random rnd) {
        int[] a = new int[N];
        for (int i = 0; i < N; i++) a[i] = rnd.nextInt(1_000_000);
        return a;
    }
    static int[] fullySorted(Random rnd) { int[] a = random(rnd); Arrays.sort(a); return a; }
    static int[] reversed(Random rnd) {
        int[] a = fullySorted(rnd);
        for (int i = 0, j = N - 1; i < j; i++, j--) { int t = a[i]; a[i] = a[j]; a[j] = t; }
        return a;
    }
    static int[] partiallySorted(Random rnd) {
        int[] a = fullySorted(rnd);
        int scattered = N / 5; // 20% random elements
        for (int k = 0; k < scattered; k++) a[rnd.nextInt(N)] = rnd.nextInt(1_000_000);
        return a;
    }

    static boolean isSorted(int[] a) {
        for (int i = 1; i < a.length; i++) if (a[i - 1] > a[i]) return false;
        return true;
    }

    static long run(String algo, int[] original) {
        int[] t = Arrays.copyOf(original, original.length);
        long start = System.nanoTime();
        switch (algo) {
            case "Selection Sort": selectionSort(t); break;
            case "Insertion Sort": insertionSort(t); break;
            case "Bubble Sort":    bubbleSort(t); break;
            case "Merge Sort":     mergeSort(t, 0, t.length - 1); break;
            case "Quick Sort":     quickSort(t, 0, t.length - 1); break;
        }
        long end = System.nanoTime();
        if (!isSorted(t)) throw new IllegalStateException(algo + " failed");
        return (end - start) / 1000;
    }

    static void benchmark() {
        Random rnd = new Random(42);
        String[] names = {"Random", "Reversed", "Partially", "Fully"};
        int[][] datasets = { random(rnd), reversed(rnd), partiallySorted(rnd), fullySorted(rnd) };
        String[] algos = {"Selection Sort", "Insertion Sort", "Bubble Sort", "Merge Sort", "Quick Sort"};

        // JIT warm-up on small data (not measured)
        int[] warm = new int[2000];
        for (int w = 0; w < 5; w++) {
            for (int i = 0; i < warm.length; i++) warm[i] = rnd.nextInt(10000);
            for (String al : algos) run(al, warm);
        }

        long[][] res = new long[algos.length][names.length];
        for (int i = 0; i < algos.length; i++)
            for (int j = 0; j < names.length; j++) {
                res[i][j] = run(algos[i], datasets[j]);
                System.err.printf("done %s / %s: %,d us%n", algos[i], names[j], res[i][j]);
            }

        System.out.printf("%-16s %18s %18s %18s %18s%n", "Algorithm", "Random", "Reversed", "Partially", "Fully");
        for (int i = 0; i < algos.length; i++) {
            System.out.printf("%-16s", algos[i]);
            for (int j = 0; j < names.length; j++) System.out.printf(" %15d us", res[i][j]);
            System.out.println();
        }
        StringBuilder csv = new StringBuilder();
        for (int i = 0; i < algos.length; i++) {
            csv.append(algos[i]);
            for (int j = 0; j < names.length; j++) csv.append(',').append(res[i][j]);
            csv.append('\n');
        }
        System.out.println("CSV_BEGIN\n" + csv + "CSV_END");
    }

    public static void main(String[] args) throws Exception {
        // Large stack: quick sort with last-element pivot recurses N deep on sorted/reversed input
        Thread t = new Thread(null, SortBenchmark::benchmark, "bench", 1L << 30);
        t.start();
        t.join();
    }
}
