import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.PolyAll;

class TestPolyAll2 {
    // @PolyAll should apply to every type that has no explicit qualifier

    public static boolean noDuplicates1(@PolyAll @NonNull @UnknownKeyFor String[] a) {
        // non-null
        a[0].hashCode();
        // :: error: (assignment.type.incompatible)
        a[0] = null;
        return true;
    }

    public static boolean noDuplicates2(@PolyAll @UnknownKeyFor @Nullable String[] a) {
        // nullable
        a[0] = null;
        // :: error: (dereference.of.nullable)
        a[0].hashCode();
        return true;
    }

    // Ensure that ordering of qualifiers doesn't matter.
    public static boolean noDuplicates3(@NonNull @PolyAll String[] a) {
        return false;
    }

    // Real duplicate forbidden.
    // :: error: (type.invalid.conflicting.annos)
    public static boolean noDuplicates4(@NonNull @PolyAll @Nullable String[] a) {
        return true;
    }
}
