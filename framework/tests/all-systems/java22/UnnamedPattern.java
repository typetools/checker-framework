// @below-java22-jdk-skip-test

// None of the WPI formats supports the new Java 22 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test
public class UnnamedPattern {

  public sealed interface IntOrBool {}

  public record WrappedInt(int a) implements IntOrBool {}

  public record WrappedBoolean(boolean b) implements IntOrBool {}

  public int test(IntOrBool i) {
    int x = 0;
    return switch (i) {
      case WrappedInt(_) -> {
        x = x + 1;
        yield x;
      }
      case WrappedBoolean(_) -> {
        x = x + 2;
        yield x;
      }
    };
  }
}
