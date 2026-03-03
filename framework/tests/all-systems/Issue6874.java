package open.caughtcrash;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

public class Issue6874 {
  public static void test(Comparator<Map[]> x, Collection<Map[]> y, Collection<? extends Map[]> z) {
    java.util.Collections.min((true) ? y : z, x);
  }
}
