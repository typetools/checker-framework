import org.checkerframework.checker.nullness.qual.*;

public class EnumsNullness {

  enum MyEnum {
    A,
    B,
    C,
    D
  }
  // :: error: (assignment.type.incompatible)
  MyEnum myEnum = null; // invalid
  @Nullable MyEnum myNullableEnum = null;

  void testLocalEnum() {
    // Enums are allowed to be null:  no error here.
    MyEnum myNullableEnum = null;
    // :: error: (assignment.type.incompatible)
    @NonNull MyEnum myEnum = null; // invalid
  }

  enum EnumBadAnnos {
    A,
    // :: error: (nullness.on.enum)
    @NonNull B,
    // :: error: (nullness.on.enum)
    @Nullable C,
    D;

    public static final EnumBadAnnos A2 = A;
    public static final @NonNull EnumBadAnnos B2 = B;
    public static final @Nullable EnumBadAnnos C2 = C;

    @Nullable String method() {
      return null;
    }
  }
}
