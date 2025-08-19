import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue406 {
  static class LocalDate {}

  private void testFails1(@Nullable LocalDate x1, @Nullable LocalDate x2) {
    boolean eitherIsNull = x1 == null || x2 == null;
    if (eitherIsNull) return;
    delegate(x1, x2);
  }

  private void testFails1b(@Nullable LocalDate x1, @Nullable LocalDate x2) {
    boolean eitherIsNull = x1 == null || x2 == null;
    if (!eitherIsNull) {
      delegate(x1, x2);
    }
  }

  private void testFails2(@Nullable LocalDate x1, @Nullable LocalDate x2) {
    boolean firstIsNull = x1 == null;
    boolean secondIsNull = x2 == null;
    if (firstIsNull || secondIsNull) return;
    delegate(x1, x2);
  }

  private void testFails2b(@Nullable LocalDate x1, @Nullable LocalDate x2) {
    boolean firstIsNull = x1 == null;
    boolean secondIsNull = x2 == null;
    if (!firstIsNull) {
      @NonNull LocalDate z = x1;
    }

    if (firstIsNull || secondIsNull) return;
    delegate(x1, x2);
  }

  private void test3(@Nullable LocalDate x1, @Nullable LocalDate x2) {
    boolean firstIsNull = x1 != null;
    boolean secondIsNull = x2 != null;
    if (!firstIsNull || !secondIsNull) {
      // ::  error: (argument)
      delegate(
          x1,
          // ::  error: (argument)
          x2);
    }
  }

  private void testWorks(@Nullable LocalDate x1, @Nullable LocalDate x2) {
    if (x1 == null || x2 == null) return;
    delegate(x1, x2);
  }

  private void delegate(LocalDate x1, LocalDate x2) {
    // do something
  }
}
