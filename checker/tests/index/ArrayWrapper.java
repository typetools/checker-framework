// Test case for https://github.com/kelloggm/checker-framework/issues/154
// This class wraps an array, but doesn't expose the array in its public interface. This test
// ensures that indexes for this new collection can be annotated as if the collection were an array.

// Note that there is a copy of this code in the manual in index-checker.tex. If this code is
// updated, you MUST update that copy, as well.

import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.LengthOf;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.SameLen;

/** ArrayWrapper is a fixed-size generic collection. */
public class ArrayWrapper<T> {
    private final Object @SameLen("this") [] delegate;

    @SuppressWarnings("index") // constructor creates object of size @SameLen(this) by definition
    ArrayWrapper(@NonNegative int size) {
        delegate = new Object[size];
    }

    public @LengthOf("this") int size() {
        return delegate.length;
    }

    public void set(@IndexFor("this") int index, T obj) {
        delegate[index] = obj;
    }

    @SuppressWarnings("unchecked") // required for normal Java compilation due to unchecked cast
    public T get(@IndexFor("this") int index) {
        return (T) delegate[index];
    }

    public static void clearIndex1(ArrayWrapper<? extends Object> a, @IndexFor("#1") int i) {
        a.set(i, null);
    }

    public static void clearIndex2(ArrayWrapper<? extends Object> a, int i) {
        if (0 <= i && i < a.size()) {
            a.set(i, null);
        }
    }

    public static void clearIndex3(ArrayWrapper<? extends Object> a, @NonNegative int i) {
        if (i < a.size()) {
            a.set(i, null);
        }
    }
}
