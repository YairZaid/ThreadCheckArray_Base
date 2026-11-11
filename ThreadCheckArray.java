import java.util.ArrayList;

/**
 * A worker that searches, by recursion, for a subset of the numbers in a
 * {@link SharedData} whose sum equals its target value.
 * <p>
 * Two workers split the search on the last element: the thread named
 * {@code "thread1"} explores the subsets that include it, and any other thread
 * explores the subsets that exclude it. Whichever finds a solution first sets
 * the shared flag, which makes the other stop, and publishes its
 * {@code boolean[]} win array to the shared data.
 */
public class ThreadCheckArray implements Runnable {

    /** {@code true} once this worker has found a solution. */
    private boolean flag;

    /**
     * This worker's marks of which elements are in its solution, sized to
     * {@code array.size()}.
     */
    private final boolean[] winArray;

    /** The state shared with the other worker. */
    private final SharedData sd;

    /** The numbers to search, taken from {@link #sd} as an {@code ArrayList<Integer>}. */
    private final ArrayList<Integer> array;

    /** The target sum, taken from {@link #sd}. */
    private final int b;

    /**
     * Creates a worker over the given shared data. The numbers and the target
     * are read once, and the {@code boolean[]} win array is created with the
     * length {@code array.size()}.
     *
     * @param sd the shared state that holds the numbers, the target sum and
     *           the result
     */
    public ThreadCheckArray(SharedData sd) {
        this.sd = sd;
        synchronized (sd) {
            array = sd.getArray();
            b = sd.getB();
        }
        winArray = new boolean[array.size()];
    }

    /**
     * Recursively checks whether some subset of the first {@code n} elements
     * of {@link #array} sums to {@code b}. When a solution is found, the
     * elements in it are marked in {@link #winArray}, and both this worker's
     * flag and the shared flag are set. It returns early as soon as the shared
     * flag shows that some worker has already succeeded.
     *
     * @param n the number of leading elements still to consider (at least 1)
     * @param b the sum still to be reached with those elements
     */
    void rec(int n, int b) {
        synchronized (sd) {
            if (sd.getFlag()) return;
        }
        if (n == 1) {
            if(b == 0 || b == array.get(n-1)) {
                flag = true;
                synchronized (sd) { sd.setFlag(true); }
            }
            if (b == array.get(n-1)) winArray[n-1] = true;
            return;
        }

        rec(n-1, b - array.get(n-1));
        if (flag) winArray[n-1] = true;
        synchronized (sd) {
            if (sd.getFlag()) return;
        }
        rec(n-1, b);
    }

    /**
     * Runs this worker's half of the search. The thread named
     * {@code "thread1"} assumes the last element is in the subset; any other
     * thread assumes it is not. A one-element list is checked directly. If
     * this worker finds a solution, its win array is published to the
     * shared data.
     */
    public void run() {
        if (array.size() != 1) {
            if (Thread.currentThread().getName().equals("thread1")) {
                rec(array.size()-1, b - array.get(array.size() - 1));
            } else {
                rec(array.size()-1, b);
            }
        }
        if (array.size() == 1 && b == array.get(0) && !flag) {
            winArray[0] = true;
            flag = true;
            synchronized (sd) { sd.setFlag(true); }
        }
        if (flag) {
            if (Thread.currentThread().getName().equals("thread1")) {
                winArray[array.size() - 1] = true;
            }
            synchronized (sd) { sd.setWinArray(winArray); }
        }
    }
}
