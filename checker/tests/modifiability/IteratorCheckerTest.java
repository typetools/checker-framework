import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.modifiability.qual.IteratorPolyMod;
import org.checkerframework.checker.modifiability.qual.MaybeIteratorPolyMod;
import org.checkerframework.checker.modifiability.qual.PolyIteratorPolyMod;
import org.checkerframework.checker.modifiability.qual.PreservesModifiability;

class IteratorCheckerTest {

  void iteratorPolyAssignment(@MaybeIteratorPolyMod List<String> maybe) {
    // :: error: [assignment]
    @IteratorPolyMod List<String> iteratorPoly = maybe;
  }

  static class BaseList extends ArrayList<String> {
    void requiresIteratorPoly(@IteratorPolyMod BaseList this) {}
  }

  static class BadOverride extends BaseList {
    @Override
    void requiresIteratorPoly(BadOverride this) {}
  }

  static class GoodOverride extends BaseList {
    @Override
    void requiresIteratorPoly(@IteratorPolyMod GoodOverride this) {}
  }

  @PreservesModifiability
  static <T> List<T> annotated(Collection<T> values) {
    return new ArrayList<>(values);
  }

  static <T> @PolyIteratorPolyMod List<T> identityIteratorPoly(
      @PolyIteratorPolyMod List<T> values) {
    return values;
  }

  void preservesIteratorPoly(
      @IteratorPolyMod List<String> iteratorPoly, @MaybeIteratorPolyMod List<String> maybe) {
    @IteratorPolyMod List<String> preserved = annotated(iteratorPoly);

    // :: error: [assignment]
    @IteratorPolyMod List<String> notPreserved = annotated(maybe);
  }

  void polyIteratorPoly(
      @IteratorPolyMod List<String> iteratorPoly, @MaybeIteratorPolyMod List<String> maybe) {
    @IteratorPolyMod List<String> preserved = identityIteratorPoly(iteratorPoly);

    // :: error: [assignment]
    @IteratorPolyMod List<String> notPreserved = identityIteratorPoly(maybe);
  }
}
