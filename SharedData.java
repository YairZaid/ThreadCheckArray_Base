import java.util.ArrayList;

/**
 * Holds the state shared by the {@link ThreadCheckArray} worker threads while
 * they search for a subset of numbers whose sum equals a target value.
 * <p>
 * The input numbers are stored in an {@code ArrayList<Integer>}. The result of
 * the search is exposed through a {@code boolean[]} "win array" in which each
 * entry marks whether the number at the same index belongs to the solution.
 * <p>
 * This class performs no synchronization itself; callers are expected to
 * synchronize on the {@code SharedData} instance when accessing it from
 * multiple threads.
 */
public class SharedData {

    /** The numbers to search, as an {@code ArrayList<Integer>}. */
    private final ArrayList<Integer> array;

    /**
     * Marks which elements of {@link #array} form the found solution.
     * Its length matches {@code array.size()}; {@code null} until a solution
     * has been published.
     */
    private boolean[] winArray;

    /** {@code true} once any thread has found a solution. */
    private boolean flag;

    /** The target sum that a subset of {@link #array} must reach. */
    private final int b;

    /**
     * Creates the shared state for a subset-sum search.
     *
     * @param array the numbers to search, as an {@code ArrayList<Integer>}
     * @param b     the target sum
     */
    public SharedData(ArrayList<Integer> array, int b) {
        this.array = array;
        this.b = b;
    }

    /**
     * Returns the array that marks the elements of the solution.
     *
     * @return a {@code boolean[]} whose length equals {@code getArray().size()},
     *         where {@code true} means the element at that index is part of the
     *         solution; {@code null} if no solution has been published
     */
    public boolean[] getWinArray() { return winArray; }

    /**
     * Publishes the array that marks the elements of the solution.
     *
     * @param winArray a {@code boolean[]} whose length should equal
     *                 {@code getArray().size()}
     */
    public void setWinArray(boolean[] winArray) { this.winArray = winArray; }

    /**
     * Returns the numbers being searched.
     *
     * @return the input numbers as an {@code ArrayList<Integer>}
     */
    public ArrayList<Integer> getArray() { return array; }

    /**
     * Returns the target sum.
     *
     * @return the value a subset of the numbers must add up to
     */
    public int getB() { return b; }

    /**
     * Tells whether a solution has been found.
     *
     * @return {@code true} if any thread has found a solution
     */
    public boolean getFlag() { return flag; }

    /**
     * Records whether a solution has been found.
     *
     * @param flag {@code true} when a thread finds a solution
     */
    public void setFlag(boolean flag) { this.flag = flag; }
}
