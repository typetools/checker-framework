import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * nxx is nullable in the record but non-null in constructor and accessor via stubs nsxx is nullable
 * in the stubs but non-null in constructor and accessor via stubs xnn is de-facto non-null in
 * record but nullable in constructor and accessor via stubs
 */
public record RecordStubbed(@Nullable String nxx, String nsxx, Integer xnn) {
  RecordStubbed(Integer a, String b, String c) {
    this(c, b, a);
  }
}
