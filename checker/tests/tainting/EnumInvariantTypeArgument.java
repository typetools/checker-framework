import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

// The type argument of `Enum<E>`, and of the `Comparable<E>` that it implements, has a qualifier
// that is copied from the enum class E, so type inference must not compare it to the qualifier of
// another type argument; see EnumSupertypeTypeArgument.java in the Resource Leak Checker's tests.
// An interface that the enum declaration itself parameterizes by E is different: its type argument
// is invariant and its qualifier is written, so the qualifiers must match.
public class EnumInvariantTypeArgument {

  interface Box<T> {}

  enum MyEnum implements Box<@Tainted MyEnum> {
    ONE,
    TWO
  }

  static <T extends MyEnum> T create() {
    throw new RuntimeException();
  }

  void useDifferentQualifiers() {
    // MyEnum implements Box<@Tainted MyEnum>, not Box<@Untainted MyEnum>.
    // :: error: (assignment) :: error: (type.arguments.not.inferred)
    Box<@Untainted MyEnum> box = create();
  }

  void useSameQualifiers() {
    Box<@Tainted MyEnum> box = create();
  }

  static <T extends Comparable<T>> T max(T a, T b) {
    return a.compareTo(b) < 0 ? b : a;
  }

  void useComparableBound() {
    // The qualifier on the type argument of `Comparable<MyEnum>` is a copy of the one on MyEnum,
    // even though MyEnum also has a written self-parameterized supertype.
    MyEnum x = max(MyEnum.ONE, MyEnum.TWO);
  }
}
