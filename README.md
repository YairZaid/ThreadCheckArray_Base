# ThreadCheckArray

A Java console program that solves the **Subset Sum** problem with a recursive
search split across two threads. Given a list of integers and a target value
`b`, it finds out whether some subset of the numbers adds up to exactly `b` and,
if so, prints which numbers make up that subset.

---

## Table of contents

1. [Project structure](#project-structure)
2. [Building and running](#building-and-running)
3. [The algorithm](#1-the-recursive-multithreaded-subset-sum-algorithm)
4. [Refactoring to `ArrayList<Integer>`](#2-data-structure-refactoring-to-arraylistinteger)
5. [Javadoc documentation](#3-code-documentation-with-javadoc)
6. [Authors and Git collaboration](#4-authors-and-git-collaboration-roles)
7. [License](#5-license)

---

## Project structure

```
ThreadCheckArray_Base/
├── README.md
└── src/
    ├── SharedData.java            # State shared by both threads
    ├── ThreadCheckArray.java      # Runnable worker: the recursive search
    └── TestThreadCheckArray.java  # Entry point: input, threads, output
```

| Class | Role |
|---|---|
| `SharedData` | Holds the input numbers (`ArrayList<Integer>`), the target `b`, a shared "solution found" flag, and the published result (`boolean[] winArray`). |
| `ThreadCheckArray` | Implements `Runnable`. Each instance searches one half of the solution space recursively. |
| `TestThreadCheckArray` | Reads input from the console, starts two worker threads, waits for them, and prints the result. |

---

## Building and running

Requires a JDK (Java 8 or later).

```bash
cd src
javac -d out *.java
java -cp out TestThreadCheckArray
```

### Example session

```
Enter array size
4
Enter numbers for array
12 5 300 7
Enter number
312
Solution for b : 312,n = 4
I:    0    1    2    3
A:    12   5    300  7
C:    0    1    1    1
```

- **I**: the index of each element
- **A**: the input numbers
- **C**: `1` if the number is part of the solution, `0` otherwise (here 5 + 300 + 7 = 312)

If no subset reaches the target, the program prints `Sorry`.

> **Note:** when a target can be reached by more than one subset, repeated runs
> may print different (equally correct) solutions. Both threads search at the
> same time, and whichever finds a solution first decides the output.

---

## 1. The recursive multithreaded Subset Sum algorithm

### The recursion

For each element there are two choices: include it in the subset or leave it
out. `ThreadCheckArray.rec(n, b)` asks: *can some subset of the first `n`
elements sum to `b`?*

```
rec(n, b):
    if another thread already found a solution → stop
    if n == 1:
        success if b == 0 (take nothing) or b == array[0] (take element 0)
        mark element 0 in winArray if it was taken
        return
    rec(n - 1, b - array[n-1])     // branch 1: include element n-1
    if a solution was found → mark element n-1 in winArray
    if another thread already found a solution → stop
    rec(n - 1, b)                  // branch 2: exclude element n-1
```

The search is exhaustive (it can explore up to 2ⁿ subsets) but stops as soon as
any solution is found.

### Splitting the work between two threads

The very first choice, about the **last** element, is split between the two
threads, so each one explores half of the search tree:

| Thread | Assumption about the last element | First call |
|---|---|---|
| `thread1` | It **is** in the subset | `rec(size - 1, b - array[size - 1])` |
| `thread2` | It is **not** in the subset | `rec(size - 1, b)` |

When the list has only one element, there is nothing to split, so each worker
checks `b == array[0]` directly.

When `thread1` succeeds, it also marks the last element in its `winArray`,
because its whole half of the search assumed that element was included.

### Thread synchronization

Both workers share one `SharedData` object. Every access to its mutable state
is wrapped in `synchronized (sd) { ... }`:

- **Reading the input:** the constructor reads `array` and `b` inside a
  synchronized block.
- **Early termination:** `rec` checks `sd.getFlag()` before each branch. As soon
  as one thread sets the shared flag, the other thread notices on its next check
  and unwinds without doing more work.
- **Publishing the result:** the winning thread calls `sd.setFlag(true)` and
  `sd.setWinArray(winArray)` while holding the lock.

Each worker keeps its **own** private `flag` and `winArray` while searching, so
the two threads never write to the same array. Only the finished result is
copied to the shared object.

The main thread calls `join()` on both workers before reading the result, which
guarantees that it sees everything the workers wrote. If the main thread is
interrupted while waiting, it restores the interrupt status, prints an error
message and exits instead of printing a partial result.

The fields `sd`, `array` and `b` in `ThreadCheckArray` are `private final`. In
particular, the lock object `sd` can never be reassigned, so both threads always
synchronize on the same monitor.

---

## 2. Data structure refactoring to `ArrayList<Integer>`

The input numbers used to be stored in a fixed-size `int[] array`. They are now
stored in an `ArrayList<Integer>`.

**Scope:** only the variable named `array` changed type. Every other array in
the project, in particular `boolean[] winArray`, is still a primitive array.

| Location | Before | After |
|---|---|---|
| `SharedData` field and constructor | `int[] array` | `ArrayList<Integer> array` |
| `SharedData.getArray()` | returns `int[]` | returns `ArrayList<Integer>` |
| `ThreadCheckArray` field | `int[] array` | `ArrayList<Integer> array` |
| `TestThreadCheckArray` input | `new int[num]`, `array[i] = ...` | `new ArrayList<>(num)`, `array.add(...)` |
| Element access | `array[i]` | `array.get(i)` |
| Length | `array.length` | `array.size()` |

**Sizing the boolean arrays:** `winArray` is created from the size of the list,
so it always has one entry per number:

```java
winArray = new boolean[array.size()];
```

**Autoboxing:** expressions such as `b == array.get(n - 1)` compare an `int`
with an `Integer`. Java unboxes the `Integer` automatically, so this compares
values, not object references.

---

## 3. Code documentation with Javadoc

Every class, field, constructor and method has a Javadoc comment, with `@param`
and `@return` tags where they apply.

### Formatting generic types

Javadoc comments are HTML. Writing a generic type as plain text, like
`ArrayList<Integer>`, makes Javadoc read `<Integer>` as an unknown HTML tag and
fail with an error. Every generic type in the comments is therefore wrapped in
`{@code ...}`:

```java
/**
 * Returns the numbers being searched.
 *
 * @return the input numbers as an {@code ArrayList<Integer>}
 */
public ArrayList<Integer> getArray() { return array; }
```

The HTML-entity form (`ArrayList&lt;Integer&gt;`) would also work, but
`{@code ...}` is easier to read and also renders the text in a code font.

### Generating the documentation

```bash
cd src
javadoc -private -Xdoclint:all -d ../docs *.java
```

`-private` includes the private members, and `-Xdoclint:all` turns on every
documentation check. The project currently generates with **0 errors and 0
warnings**. Open `docs/index.html` to browse the result.

---

## 4. Authors and Git collaboration roles

| Name | GitHub | Role |
|---|---|---|
| Yair Zaid | [@YairZaid](https://github.com/YairZaid) | Repository owner. Created the base threading project and carried out the `ArrayList<Integer>` refactor and Javadoc. |
| _[Team member name]_ | _[@username]_ | _[Role, e.g. code reviewer / tester]_ |
| _[Team member name]_ | _[@username]_ | _[Role]_ |

Repository: <https://github.com/YairZaid/ThreadCheckArray_Base>

### Commit history

| Commit | Description |
|---|---|
| `f8d5119` | Initial commit: base threading project |
| `e7efc08` | Refactor `SharedData` and `ThreadCheckArray` to `ArrayList<Integer>` with Javadoc |

### Workflow

_[Describe how the team works together, for example: each change is made on a
feature branch, opened as a pull request, reviewed by another member, then
merged into `master`.]_

---

## 5. License

This project is licensed under the **Braude College Academic License**.

It was created as academic coursework at Braude College of Engineering. See
the [`LICENSE`](LICENSE) file for the full terms. _[Add the license text provided
by the college to a `LICENSE` file in the project root.]_
