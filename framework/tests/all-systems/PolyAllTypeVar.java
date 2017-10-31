import org.checkerframework.common.value.qual.MinLen;
import org.checkerframework.framework.qual.PolyAll;

public final class PolyAllTypeVar<T extends Comparable<T>> {
    public void method(@PolyAll T @MinLen(1) [] a1, @PolyAll T[] a2) {
        // Type systems that don't support dataflow will issue a warning here
        @SuppressWarnings({"guieffect", "units"})
        T elt1 = a1[0];
    }
}
