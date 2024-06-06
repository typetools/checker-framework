import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue402 {
  static class LocalDate {}

  private void testFails1(@Nullable LocalDate x1, @Nullable LocalDate x2) {
    boolean eitherIsNull = x1 == null || x2 == null;
    if (eitherIsNull) return;
    delegate(x1, x2);
  }

  private void testFails2(@Nullable LocalDate x1, @Nullable LocalDate x2) {
    boolean firstIsNull = x1 == null;
    boolean secondIsNull = x2 == null;
    if (firstIsNull || secondIsNull) return;
    delegate(x1, x2);
  }

  private void testWorks(@Nullable LocalDate x1, @Nullable LocalDate x2) {
    if (x1 == null || x2 == null) return;
    delegate(x1, x2);
  }

  private void delegate(LocalDate x1, LocalDate x2) {
    // do something
  }
}
