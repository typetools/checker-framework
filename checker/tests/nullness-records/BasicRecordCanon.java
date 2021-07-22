import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java16-jdk-skip-test
public record BasicRecordCanon(String str) {

  public static BasicRecordCanon makeNonNull(String s) {
    return new BasicRecordCanon(s);
  }

  public static BasicRecordCanon makeNull(@Nullable String s) {
    // :: error: argument
    return new BasicRecordCanon(s);
  }

  public BasicRecordCanon {}
}
