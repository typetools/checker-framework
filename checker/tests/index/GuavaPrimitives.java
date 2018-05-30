import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.IndexOrLow;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.LessThan;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.common.value.qual.MinLen;

/**
 * A simplified version of the Guava primitives classes (such as Bytes, Longs, Shorts, etc.) with
 * all expected warnings suppressed.
 */
public class GuavaPrimitives {
    final short @MinLen(1) @SameLen(value = "this", offset = "this.start") [] array;
    final @IndexOrHigh("array") @LessThan("this.end") int start;
    final @IndexOrHigh("array") int end;

    public static @IndexOrLow("#1") int indexOf(short[] array, short target) {
        return indexOf(array, target, 0, array.length);
    }

    private static @IndexOrLow("#1") int indexOf(
            short[] array, short target, @IndexOrHigh("#1") int start, @IndexOrHigh("#1") int end) {
        for (int i = start; i < end; i++) {
            if (array[i] == target) {
                return i;
            }
        }
        return -1;
    }

    public static @IndexOrLow("#1") int lastIndexOf(short[] array, short target) {
        return lastIndexOf(array, target, 0, array.length);
    }

    private static @IndexOrLow("#1") int lastIndexOf(
            short[] array, short target, @IndexOrHigh("#1") int start, @IndexOrHigh("#1") int end) {
        for (int i = end - 1; i >= start; i--) {
            if (array[i] == target) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings(
            "lessthan:argument.type.incompatible") // https://github.com/kelloggm/checker-framework/issues/225
    GuavaPrimitives(short @MinLen(1) [] bar) {
        this(bar, 0, bar.length);
    }

    @SuppressWarnings(
            "samelen:assignment.type.incompatible") // https://github.com/kelloggm/checker-framework/issues/213
    GuavaPrimitives(
            short @MinLen(1) [] array,
            @IndexOrHigh("#1") @LessThan("this.end") int start,
            @IndexOrHigh("#1") int end) {
        this.array = array;
        this.start = start;
        this.end = end;
    }

    @SuppressWarnings({"upperbound:return.type.incompatible"}) // custom coll. with size end-start
    public @Positive @LTLengthOf(
        value = {"this", "array"},
        offset = {"0", "start - 1"}
    ) int size() { // INDEX: Annotation on a public method refers to private member.
        return end - start;
    }

    public boolean isEmpty() {
        return false;
    }

    public Short get(@IndexFor("this") int index) {
        return array[start + index];
    }

    @SuppressWarnings({
        "lowerbound:return.type.incompatible", // custom coll. with size end-start: indexOf returns a value that is greater than its third argument
        "upperbound:return.type.incompatible"
    }) // SameLen with offsets is not reflexive
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

    @SuppressWarnings({
        "lowerbound:return.type.incompatible", // custom coll. with size end-start: indexOf returns a value that is greater than its third argument
        "upperbound:return.type.incompatible"
    }) // SameLen with offsets is not reflexive
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

    public List<Short> subList(
            @IndexOrHigh("this") @LessThan("#2") int fromIndex, @IndexOrHigh("this") int toIndex) {
        int size = size();
        if (fromIndex == toIndex) {
            return Collections.emptyList();
        }
        GuavaPrimitives g = new GuavaPrimitives(array, start + fromIndex, start + toIndex);
        return null;
    }
}
