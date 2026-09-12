import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

// The supertype of an enum class E is `Enum<E>`, whose type argument is E itself, annotated with
// the same qualifier as E.  That qualifier is a copy of the qualifier on E rather than an
// invariant type argument, so type inference must not compare it to the qualifier of another type
// argument.
public class EnumSupertypeTypeArgument {
  enum MyEnum {
    ONE,
    TWO
  }

  static <T extends Enum<T>> T id(T value) {
    return value;
  }

  void useDifferentQualifiers(@Untainted MyEnum arg) {
    // T is @Untainted MyEnum, whose supertype is Enum<@Untainted MyEnum>, whereas the supertype
    // of the target type @Tainted MyEnum is Enum<@Tainted MyEnum>.
    @Tainted MyEnum x = id(arg);
  }

  void useSameQualifiers(@Untainted MyEnum arg) {
    @Untainted MyEnum x = id(arg);
  }

  static <T extends Comparable<T>> T max(T a, T b) {
    return a.compareTo(b) < 0 ? b : a;
  }

  void useComparableBound(@Untainted MyEnum a, @Untainted MyEnum b) {
    // The synthesized type argument of `Enum<MyEnum>` also appears in `Comparable<MyEnum>`, which
    // `Enum<MyEnum>` implements.
    @Tainted MyEnum x = max(a, b);
  }
}
