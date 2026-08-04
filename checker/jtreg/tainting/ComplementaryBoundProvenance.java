/*
 * @test
 * @summary Test that the constraint history in a type argument inference error message attributes
 * "From complementary bound." to the constraint that was created by incorporating a complementary
 * pair of bounds, rather than to that constraint's parent.
 *
 * @compile/fail/ref=ComplementaryBoundProvenance.out -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker ComplementaryBoundProvenance.java
 */

import java.util.ArrayList;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class ComplementaryBoundProvenance {
  static <X, T extends Iterable<X>> void m(T t, X x) {}

  void test(ArrayList<@Untainted String> untainted, @Tainted String tainted) {
    m(untainted, tainted);
  }
}
