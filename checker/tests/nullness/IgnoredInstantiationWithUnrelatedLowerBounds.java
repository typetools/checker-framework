import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// T = X comes from a use with an explicit annotation, so it ignores annotations, and resolution
// takes T's annotations from its lower bounds.  The least upper bound of those lower bounds is not
// the instantiation's Java type X: it is a different parameterization of X, a subtype of X, or a
// raw type.
public class IgnoredInstantiationWithUnrelatedLowerBounds {
  static class Box<T> {}

  static <T extends @Nullable Object> T pick(Box<@NonNull T> b, T x, T y) {
    return x;
  }

  static void wildcards(
      Box<@NonNull List<? extends Object>> bWild,
      Box<@NonNull List<?>> bUnbounded,
      @Nullable List<String> nls,
      List<Integer> li) {
    // :: error: (dereference.of.nullable)
    pick(bWild, nls, li).toString();
    // :: error: (dereference.of.nullable)
    pick(bUnbounded, nls, li).toString();
    // :: error: (dereference.of.nullable)
    pick(bWild, li, nls).toString();
  }

  @SuppressWarnings("rawtypes")
  static void raw(
      Box<@NonNull List> bRaw, @Nullable ArrayList<String> nal, LinkedList<Integer> ll) {
    // :: error: (dereference.of.nullable)
    pick(bRaw, nal, ll).toString();
  }

  static void classes(
      Box<@NonNull Object> bObj, @Nullable ArrayList<String> nal, LinkedList<Integer> ll) {
    // :: error: (dereference.of.nullable)
    pick(bObj, nal, ll).toString();
  }
}
