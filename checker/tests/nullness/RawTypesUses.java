import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class RawTypesUses {
    class Generic<G extends @Nullable Object> {
        G foo() {
            throw new RuntimeException();
        }
    }

    void foo() {
        Generic<@Nullable String> notRawNullable = new Generic<>();
        // :: error: (assignment.type.incompatible)
        @NonNull Object o1 = notRawNullable.foo();

        Generic rawNullable = new Generic<@Nullable String>();
        // TODO: false negative. See #635.
        //// :: error: (assignment.type.incompatible)
        @NonNull Object o2 = rawNullable.foo();

        Generic<@NonNull String> notRawNonNull = new Generic<>();
        @NonNull Object o3 = notRawNonNull.foo();

        Generic rawNonNull = new Generic<@NonNull String>();
        Generic rawNonNullAlais = rawNonNull;
        // TODO: false negative. See #635.
        //// :: error: (assignment.type.incompatible)
        @NonNull Object o4 = rawNonNull.foo();
        // TODO: false negative. See #635.
        //// :: error: (assignment.type.incompatible)
        @NonNull Object o5 = rawNonNullAlais.foo();
    }

    abstract Generic rawReturn();

    void bar() {
        // :: warning: [unchecked] unchecked conversion
        Generic<@Nullable String> notRawNullable = rawReturn();
        // :: error: (assignment.type.incompatible)
        @NonNull Object o1 = notRawNullable.foo();

        Generic rawNullable = rawReturn();
        // TODO: false negative. See #635.
        //// :: error: (assignment.type.incompatible)
        @NonNull Object o2 = rawNullable.foo();

        // TODO: false negative. See #635.
        //// :: error: (assignment.type.incompatible)
        @NonNull Object o3 = rawReturn().foo();

        Generic local = rawReturn();
        Generic localAlias = local;
        // TODO: false negative. See #635.
        //// :: error: (assignment.type.incompatible)
        @NonNull Object o4 = local.foo();
    }
}
