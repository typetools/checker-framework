import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.LengthOf;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.SameLen;

/**
 * A test for https://github.com/kelloggm/checker-framework/issues/154
 *
 * <p>This class wraps an array, but doesn't expose the array in its public interface. This test
 * ensures that indexes for this new collection can be annotated as if the collection were an array.
 *
 * <p>ArrayWrapper is a fixed-size, weakly typed generic collection.
 */
@SuppressWarnings("unchecked")
public class ArrayWrapper<T> {
    private final Object @SameLen("this") [] internalRep;

    ArrayWrapper(@NonNegative int size) {
        @SuppressWarnings(
                "index") // use the SameLen checker to ensure that anything that's an index for "this" is an
        // index for the internal array representation. This suppress warnings asserts this fact.
        final Object @SameLen("this") [] tmp = new Object[size];
        internalRep = tmp;
    }

    public @LengthOf("this") int size() {
        return internalRep.length;
    }

    public void set(@IndexFor("this") int index, T obj) {
        internalRep[index] = obj;
    }

    public T get(@IndexFor("this") int index) {
        return (T) internalRep[index];
    }

    public static void clearIndex(ArrayWrapper a, @NonNegative int i) {
        if (i < a.size()) {
            a.set(i, null);
        }
    }
}
