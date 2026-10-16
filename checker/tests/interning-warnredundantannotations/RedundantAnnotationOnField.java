import org.checkerframework.checker.interning.qual.Interned;

public class RedundantAnnotationOnField {
  static final @Interned String A_STRING = "a string";
}
