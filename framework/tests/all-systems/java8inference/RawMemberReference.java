import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Method references of the form {@code ReferenceType :: Identifier} where {@code ReferenceType} is
 * raw. JLS 15.13.1 does not infer the class's type arguments for these: "If ReferenceType is a raw
 * type, and there exists a parameterization of this type, G&lt;...&gt;, that is a supertype of P1,
 * the type to search is the result of capture conversion (5.1.10) applied to G&lt;...&gt;."
 *
 * <p>Each method below exercises a different part of that rule. See also all-systems Issue6725 and
 * Issue6769, which cover a no-argument method on a class with a single type parameter.
 */
// Checkers may correctly issue errors, so suppress them.
@SuppressWarnings("all") // Just check for crashes.
public class RawMemberReference {

  /** A formal parameter of the compile-time declaration mentions the class's type parameter. */
  static void parameterMentionsClassTypeParameter() {
    BiConsumer<List<? super String>, String> c = List::add;
  }

  /** The raw ReferenceType has more than one type parameter. */
  static void severalTypeParameters() {
    BiFunction<Map<String, ? extends Number>, Object, ? extends Number> f = Map::get;
    Function<Map<String, ? extends Number>, Set<String>> g = Map::keySet;
  }

  /**
   * The compile-time declaration is itself generic, so the class's type arguments are fixed by JLS
   * 15.13.1 while the method's type arguments are still inferred.
   */
  static void genericCompileTimeDeclaration() {
    BiFunction<List<? extends Number>, Object[], Object[]> f = List::toArray;
  }

  /**
   * P1 has no parameterization of the ReferenceType as a supertype, only the raw type, so JLS
   * 15.13.1's rule does not apply and the class's type arguments are inferred instead.
   */
  static void noParameterizedSupertype(Stream<? extends List> s) {
    s.map(List::iterator);
  }

  /** G&lt;...&gt; contains a lower-bounded wildcard. */
  static void superWildcard(Stream<? extends List<? super Integer>> s) {
    s.map(List::iterator);
  }

  /** The raw ReferenceType is an inner class. */
  static void innerClass(Stream<? extends Outer<String>.Inner<Number>> s) {
    s.map(Outer.Inner::get);
  }

  /**
   * The raw ReferenceType is an inner class, and the compile-time declaration's return type
   * mentions the enclosing class's type parameter rather than the inner class's.
   */
  static void innerClassOuterTypeParameter(Stream<? extends Outer<String>.Inner<Number>> s) {
    s.map(Outer.Inner::getOuter).forEach(x -> {});
  }

  static class Outer<A> {
    class Inner<B> {
      B get() {
        throw new RuntimeException();
      }

      A getOuter() {
        throw new RuntimeException();
      }
    }
  }
}
