import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.AnnotatedFor;

// Tests that lack of @AnnotatedFor suppresses warnings, when
// -AuseDefaultsForUnCheckedCode=source is supplied.

@AnnotatedFor("nullness")
class BasicSafeDefaultsTest {

    void m1() {
        @NonNull Object x1 = SdfuscLib.unannotated();
        // :: error: (assignment.type.incompatible)
        @NonNull Object x2 = SdfuscLib.returnsNullable();
        @NonNull Object x3 = SdfuscLib.returnsNonNull();
        // :: error: (assignment.type.incompatible)
        @NonNull Object x4 = SdfuscLibNotAnnotatedFor.unannotated();
        // :: error: (assignment.type.incompatible)
        @NonNull Object x5 = SdfuscLibNotAnnotatedFor.returnsNullable();
        @NonNull Object x6 = SdfuscLibNotAnnotatedFor.returnsNonNull();
    }

    void m2() {
        @Nullable Object x1 = SdfuscLib.unannotated();
        @Nullable Object x2 = SdfuscLib.returnsNullable();
        @Nullable Object x3 = SdfuscLib.returnsNonNull();
        @Nullable Object x4 = SdfuscLibNotAnnotatedFor.unannotated();
        @Nullable Object x5 = SdfuscLibNotAnnotatedFor.returnsNullable();
        @Nullable Object x6 = SdfuscLibNotAnnotatedFor.returnsNonNull();
    }
}

class BasicTestNotAnnotatedFor {
    void m1() {
        @NonNull Object x1 = SdfuscLib.unannotated();
        @NonNull Object x2 = SdfuscLib.returnsNullable();
        @NonNull Object x3 = SdfuscLib.returnsNonNull();
        @NonNull Object x4 = SdfuscLibNotAnnotatedFor.unannotated();
        @NonNull Object x5 = SdfuscLibNotAnnotatedFor.returnsNullable();
        @NonNull Object x6 = SdfuscLibNotAnnotatedFor.returnsNonNull();
    }

    void m2() {
        @Nullable Object x1 = SdfuscLib.unannotated();
        @Nullable Object x2 = SdfuscLib.returnsNullable();
        @Nullable Object x3 = SdfuscLib.returnsNonNull();
        @Nullable Object x4 = SdfuscLibNotAnnotatedFor.unannotated();
        @Nullable Object x5 = SdfuscLibNotAnnotatedFor.returnsNullable();
        @Nullable Object x6 = SdfuscLibNotAnnotatedFor.returnsNonNull();
    }
}
