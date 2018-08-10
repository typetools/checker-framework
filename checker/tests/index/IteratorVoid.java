import java.util.Iterator;

// If quals are configured incorrectly, there will be an
// incompatible assignment error; this ensures that Void
// is given the Positive type.

public class IteratorVoid<T> {
    T next1;
    Iterator<T> itor1;

    private void setnext1() {
        next1 = itor1.hasNext() ? itor1.next() : null;
    }
}
