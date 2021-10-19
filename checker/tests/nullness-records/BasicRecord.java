import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java17-jdk-skip-test
public record BasicRecord(String str) {

  public static BasicRecord makeNonNull(String s) {
    return new BasicRecord(s);
  }

  public static BasicRecord makeNull(@Nullable String s) {
    // :: error: argument
    return new BasicRecord(s);
  }
}
