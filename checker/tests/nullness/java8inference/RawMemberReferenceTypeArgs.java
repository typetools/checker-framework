import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * For a method reference {@code ReferenceType :: Identifier} with a raw {@code ReferenceType}, JLS
 * 15.13.1 takes the class's type arguments from capture(G&lt;...&gt;), where G&lt;...&gt; is the
 * parameterization of {@code ReferenceType} that is a supertype of P1. They are not inferred.
 *
 * <p>The all-systems tests for this only check that inference does not crash. These assignments
 * check that the type arguments are the ones JLS 15.13.1 specifies: if they were inferred, they
 * would resolve to {@code Object} and these assignments would not type-check.
 */
public class RawMemberReferenceTypeArgs {

  static <T> void typeVariableElement(Stream<? extends Iterable<? extends T>> iterables) {
    Stream<? extends Iterator<? extends T>> s = iterables.map(Iterable::iterator);
  }

  static void concreteElement(Stream<? extends List<? extends Number>> lists) {
    Stream<? extends Iterator<? extends Number>> s = lists.map(List::iterator);
  }

  /**
   * G&lt;...&gt; exists, so the rule applies and {@code List}'s type argument is taken from
   * capture(G&lt;...&gt;) rather than inferred. P1 is a capture of {@code ? extends List<@Nullable
   * String>}, so the type to search is {@code List<@Nullable String>} and the method reference has
   * type {@code Iterator<@Nullable String>}.
   */
  @SuppressWarnings("unchecked")
  static void parameterizedSupertype(Stream<? extends List<@Nullable String>> lists) {
    Stream<? extends Iterator<@Nullable String>> nullable = lists.map(List::iterator);
    // The type argument came from P1, so it is @Nullable String and not @NonNull String.
    // :: error: (assignment) :: error: (type.arguments.not.inferred)
    Stream<? extends Iterator<@NonNull String>> nonNull = lists.map(List::iterator);
  }

  /**
   * P1's only supertype at {@code List} is the raw type, so no parameterization G&lt;...&gt; exists
   * and the rule does not apply: JLS 15.13.1's second search falls back to the raw {@code List}
   * itself. Using a raw type in place of a parameterization requires unchecked conversion (JLS
   * 5.1.9), so {@code List}'s type argument is erased rather than inferred or fixed from capture.
   * An erased type argument is accepted against any parameterization, so both assignments below are
   * accepted, whereas in {@link #parameterizedSupertype} the second is an error.
   */
  @SuppressWarnings("unchecked")
  static void rawSupertype(Stream<? extends List> lists) {
    Stream<? extends Iterator<@Nullable String>> nullable = lists.map(List::iterator);
    Stream<? extends Iterator<@NonNull String>> nonNull = lists.map(List::iterator);
  }

  /**
   * The raw ReferenceType is an inner class, and the compile-time declaration's return type
   * mentions the enclosing class's type parameter rather than the inner class's. The enclosing
   * class's type argument ({@code Outer}'s {@code A}) must be fixed from capture(G&lt;...&gt;) just
   * like the inner class's ({@code Inner}'s {@code B}), so {@code getOuter()} resolves to {@code
   * String} here.
   */
  static void innerClassOuterTypeParameter(Stream<? extends Outer<String>.Inner<Number>> s) {
    Stream<? extends String> r = s.map(Outer.Inner::getOuter);
  }

  static class Outer<A> {
    class Inner<B> {
      A getOuter() {
        throw new RuntimeException();
      }
    }
  }
}
