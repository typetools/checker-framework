import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.DefaultQualifier;

public class WildcardSuper {

  void testWithSuper(Cell<? super @NonNull String> cell) {
    // :: error: (dereference.of.nullable)
    cell.get().toString();
  }

  void testWithContradiction(Cell<? super @Nullable String> cell) {
    // :: error: (dereference.of.nullable)
    cell.get().toString();
  }

  @DefaultQualifier(Nullable.class)
  void testWithImplicitNullable(@NonNull Cell<? super @NonNull String> cell) {
    // :: error: (dereference.of.nullable)
    cell.get().toString();
  }

  void testWithExplicitNullable(Cell<@Nullable ? extends @Nullable String> cell) {
    // :: error: (dereference.of.nullable)
    cell.get().toString();
  }

  void testWithDoubleNullable(Cell<@Nullable ? extends @Nullable String> cell) {
    // :: error: (dereference.of.nullable)
    cell.get().toString();
  }

  class Cell<E extends @Nullable Object> {
    E get() {
      throw new RuntimeException();
    }
  }
}
