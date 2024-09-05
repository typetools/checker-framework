import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue6785 {
  @Nullable String foo2(String x) {
    return x.isEmpty() ? null : null;
  }
}
