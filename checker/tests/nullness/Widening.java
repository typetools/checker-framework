import org.checkerframework.checker.nullness.qual.Nullable;

public class Widening {
  @Nullable Integer i;

  void inc(long amt) {}

  void foo() {
    inc(i == null ? 0 : i);
  }
}
