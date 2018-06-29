import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.index.qual.HasSubsequence;
import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.IndexOrLow;
import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.LessThan;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.common.value.qual.MinLen;

/**
 * A simplified version of the Guava primitives classes (such as Bytes, Longs, Shorts, etc.) with
 * all expected warnings suppressed.
 */
public class GuavaPrimitives extends AbstractList<Short> {
    @HasSubsequence(value = "this", from = "this.start", to = "this.end")
    final short @MinLen(1) [] array;

    final @IndexFor("array") @LessThan("end") int start;
    final @Positive @LTEqLengthOf("array") int end;

    public static @IndexOrLow("#1") int indexOf(short[] array, short target) {
        return indexOf(array, target, 0, array.length);
    }

    private static @IndexOrLow("#1") @LessThan("#4") int indexOf(
            short[] array, short target, @IndexOrHigh("#1") int start, @IndexOrHigh("#1") int end) {
        for (int i = start; i < end; i++) {
            if (array[i] == target) {
                return i;
            }
        }
        return -1;
    }

    private static @IndexOrLow("#1") @LessThan("#4") int lastIndexOf(
            short[] array, short target, @IndexOrHigh("#1") int start, @IndexOrHigh("#1") int end) {
        for (int i = end - 1; i >= start; i--) {
            if (array[i] == target) {
                return i;
            }
        }
        return -1;
    }

    GuavaPrimitives(short @MinLen(1) [] array) {
        this(array, 0, array.length);
    }

    @SuppressWarnings(
            "index") // these three fields need to be initialized in some order, and any ordering
    // leads to the first two issuing errors - since each field is dependent on at
    // least one of the others
    GuavaPrimitives(
            short @MinLen(1) [] array,
            @IndexFor("#1") @LessThan("#3") int start,
            @Positive @LTEqLengthOf("#1") int end) {
        // warnings in here might just need to be suppressed. A single @SuppressWarnings("index") to
        // establish rep. invariant might be okay?
        this.array = array;
        this.start = start;
        this.end = end;
    }

    public @Positive @LTLengthOf(
            value = {"this", "array"},
            offset = {"-1", "start - 1"}) int
            size() { // INDEX: Annotation on a public method refers to private member.
        return end - start;
    }

    public boolean isEmpty() {
        return false;
    }

    public Short get(@IndexFor("this") int index) {
        return array[start + index];
    }

    @SuppressWarnings(
            "lowerbound") // https://github.com/kelloggm/checker-framework/issues/227 indexOf()
    public @IndexOrLow("this") int indexOf(Object target) {
        // Overridden to prevent a ton of boxing
        if (target instanceof Short) {
            int i = GuavaPrimitives.indexOf(array, (Short) target, start, end);
            if (i >= 0) {
                return i - start;
            }
        }
        return -1;
    }

    @SuppressWarnings(
            "lowerbound") // https://github.com/kelloggm/checker-framework/issues/227 lastIndexOf()
    public @IndexOrLow("this") int lastIndexOf(Object target) {
        // Overridden to prevent a ton of boxing
        if (target instanceof Short) {
            int i = GuavaPrimitives.lastIndexOf(array, (Short) target, start, end);
            if (i >= 0) {
                return i - start;
            }
        }
        return -1;
    }

    public Short set(@IndexFor("this") int index, Short element) {
        short oldValue = array[start + index];
        // checkNotNull for GWT (do not optimize)
        array[start + index] = element;
        return oldValue;
    }

    @SuppressWarnings("index") // needs https://github.com/kelloggm/checker-framework/issues/229
    public List<Short> subList(
            @IndexOrHigh("this") @LessThan("#2") int fromIndex, @IndexOrHigh("this") int toIndex) {
        int size = size();
        if (fromIndex == toIndex) {
            return Collections.emptyList();
        }
        return new GuavaPrimitives(array, start + fromIndex, start + toIndex);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(size() * 6);
        builder.append('[').append(array[start]);
        for (int i = start + 1; i < end; i++) {
            builder.append(", ").append(array[i]);
        }
        return builder.append(']').toString();
    }
}
