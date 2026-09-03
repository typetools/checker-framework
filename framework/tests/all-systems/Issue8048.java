import java.util.Collection;
import java.util.List;

/**
 * Invoking a generic method whose return type is an array of a parameterized type with an {@code ?
 * extends} wildcard, in an assignment context whose target is that same array type, crashed
 * inference with a {@code FalseBoundException}. Reduced from Apache Beam's {@code
 * MoreFutures.allAsList}.
 *
 * <p>The crash happened because reducing the subtyping constraint capture-converted the supertype
 * of S before comparing type arguments, so a containment constraint between two occurrences of
 * {@code ? extends T} became one between a capture variable and a wildcard, which reduces to false.
 * JLS 18.3 states that no new capture variables are generated when reducing subtyping constraints
 * (JLS 18.2.3).
 */
// Checkers may correctly issue errors, so suppress them.
@SuppressWarnings("all") // Just check for crashes.
public class Issue8048 {

  static <T> List<? extends T>[] noArgs() {
    throw new RuntimeException();
  }

  static <T> List<? extends T>[] fromCollection(Collection<? extends T> c) {
    throw new RuntimeException();
  }

  static <T> List<? extends T>[][] twoDimensional() {
    throw new RuntimeException();
  }

  static <T> void typeVariableTarget(Collection<? extends T> c) {
    List<? extends T>[] a = noArgs();
    List<? extends T>[] b = fromCollection(c);
    List<? extends T>[][] d = twoDimensional();
  }

  static void concreteTarget() {
    List<? extends String>[] a = noArgs();
  }

  // These do not use `? extends`, so they never crashed; they guard against a regression in the
  // other direction.

  static <T> List<? super T>[] superWildcard() {
    throw new RuntimeException();
  }

  static <T> List<?>[] unboundedWildcard() {
    throw new RuntimeException();
  }

  static <T> void otherWildcards() {
    List<? super T>[] a = superWildcard();
    List<?>[] b = unboundedWildcard();
  }
}
