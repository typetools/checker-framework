package open.caughtcrash;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

public class Issue6874 {
  // Collections.min is @Pure, and Comparator.compare is not annotated, so passing `x` is a true
  // positive under -AcheckPurityAnnotations.  This test is not about purity; the expected error is
  // asserted in framework/tests/flow/PurityFunctionalArgumentJdk.java.
  @SuppressWarnings("purity")
  public static void test(Comparator<Map[]> x, Collection<Map[]> y, Collection<? extends Map[]> z) {
    java.util.Collections.min((true) ? y : z, x);
  }
}
