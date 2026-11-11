import java.util.ArrayList;
import java.util.Scanner;

/**
 * Console entry point for the two-thread subset-sum search.
 * <p>
 * Reads a list of integers into an {@code ArrayList<Integer>} and a target
 * sum, runs two {@link ThreadCheckArray} workers over a shared
 * {@link SharedData}, and prints either the solution or {@code "Sorry"} if
 * none exists.
 */
public class TestThreadCheckArray {

    /** Not meant to be created; this class only provides {@link #main(String[])}. */
    private TestThreadCheckArray() { }

    /**
     * Reads the input from standard input, runs the search and prints the
     * result as three rows: the indexes ({@code I}), the numbers
     * ({@code A}), and whether each number is in the solution ({@code C},
     * where 1 means yes).
     *
     * @param args not used
     */
    public static void main(@SuppressWarnings("unused") String[] args) {
        try (Scanner input = new Scanner(System.in)) {
            Thread thread1, thread2;
            System.out.println("Enter array size");
            int num  = input.nextInt();
            ArrayList<Integer> array = new ArrayList<>(num);
            System.out.println("Enter numbers for array");

            for (int index = 0; index < num; index++)
                array.add(input.nextInt());

            System.out.println("Enter number");
            num = input.nextInt();

            SharedData sd = new SharedData(array, num);

            thread1 = new Thread(new ThreadCheckArray(sd), "thread1");
            thread2 = new Thread(new ThreadCheckArray(sd), "thread2");
            thread1.start();
            thread2.start();
            try {
                thread1.join();
                thread2.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Interrupted while waiting for the search threads: " + e.getMessage());
                return;
            }
            if (!sd.getFlag()) {
                System.out.println("Sorry");
                return;
            }
            System.out.println("Solution for b : " + sd.getB() + ",n = " + sd.getArray().size());
            System.out.print("I:    ");
            for(int index = 0; index < sd.getArray().size() ; index++)
                System.out.print(index + "    ");
            System.out.println();
            System.out.print("A:    ");
            for (int index : sd.getArray()) {
                System.out.print(index);
                int counter = 5;
                do {
                    index = index / 10;
                    counter--;
                } while (index != 0);
                for (int i = 0; i < counter; i++) System.out.print(" ");
            }
            System.out.println();
            System.out.print("C:    ");
            for (boolean index : sd.getWinArray()) {
                if (index) System.out.print("1    ");
                else System.out.print("0    ");
            }
        }
    }
}
