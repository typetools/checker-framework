import java.io.Serializable;
import java.util.Comparator;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.qual.PolyAll;

public final class PolyAllTypeVar<T extends Comparable<T>>
        implements Comparator<T[]>, Serializable {
    static final long serialVersionUID = 20150812L;

    @Pure
    @SuppressWarnings(
            "override.param.invalid") // CF bug: does not permit expanding annotations on array elements with @Poly
    // The signature on this method is unnecessarily strict because it
    // requires that the component types be identical.  The signature should
    // be compare(@PolyAll(1) T[], @PolyAll(2) T[]), but the
    // @PolyAll qualifier does not yet take an argument.
    public int compare(@PolyAll T[] a1, @PolyAll T[] a2) {
        // Type systems that don't support dataflow will issue a warning here
        @SuppressWarnings({"guieffect", "units"})
        T elt1 = a1[0];
        return 0;
    }
}
