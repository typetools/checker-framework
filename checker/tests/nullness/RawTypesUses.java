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
    // :: error: (assignment)
    @NonNull Object o1 = notRawNullable.foo();

    Generic rawNullable = new Generic<@Nullable String>();
    // :: error: (assignment)
    @NonNull Object o2 = rawNullable.foo();

    Generic<@NonNull String> notRawNonNull = new Generic<>();
    @NonNull Object o3 = notRawNonNull.foo();

    Generic rawNonNull = new Generic<@NonNull String>();
    Generic rawNonNullAlais = rawNonNull;
    // :: error: (assignment)
    @NonNull Object o4 = rawNonNull.foo();
    // :: error: (assignment)
    @NonNull Object o5 = rawNonNullAlais.foo();
  }

  abstract Generic rawReturn();

  void bar() {
    // :: warning: [unchecked] unchecked conversion
    Generic<@Nullable String> notRawNullable = rawReturn();
    // :: error: (assignment)
    @NonNull Object o1 = notRawNullable.foo();

    Generic rawNullable = rawReturn();
    // :: error: (assignment)
    @NonNull Object o2 = rawNullable.foo();

    // :: error: (assignment)
    @NonNull Object o3 = rawReturn().foo();

    Generic local = rawReturn();
    Generic localAlias = local;
    // :: error: (assignment)
    @NonNull Object o4 = local.foo();
  }
}
