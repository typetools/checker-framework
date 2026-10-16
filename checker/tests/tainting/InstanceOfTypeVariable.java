import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

// A binding variable is refined by the effective qualifier of the expression that is tested, which
// for a type variable is the qualifier of its upper bound.
public class InstanceOfTypeVariable {

  <T extends @Untainted Object> void untaintedBound(T t) {
    if (t instanceof String s) {
      @Untainted String u = s;
    }
  }

  <T extends @Tainted Object> void taintedBound(T t) {
    if (t instanceof String s) {
      // :: error: (assignment)
      @Untainted String u = s;
    }
  }

  <T extends @Untainted Object> void wildcard(java.util.List<? extends T> list) {
    if (list.get(0) instanceof String s) {
      @Untainted String u = s;
    }
  }
}
