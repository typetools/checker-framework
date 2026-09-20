import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

// An equality constraint between two proper types that arises from an argument expression does not
// compare qualifiers, because BaseTypeVisitor separately issues a more informative error message
// about the argument.
public class ProperTypeEqualityFromArgument {

  interface Pair<A, B> {}

  <T> void m(Pair<T, @Untainted String> p, @Untainted String s) {}

  void use(Pair<String, @Tainted String> p, @Tainted String s) {
    // Both arguments have the wrong qualifier.  Each one is reported separately, rather than the
    // whole invocation being reported as "type.arguments.not.inferred".
    // :: error: [argument] :: error: [argument]
    m(p, s);
  }
}
