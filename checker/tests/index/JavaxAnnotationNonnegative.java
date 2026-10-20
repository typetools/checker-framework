// Test for https://github.com/typetools/checker-framework/issues/6507

public class JavaxAnnotationNonnegative {

  public static void test(@javax.annotation.Nonnegative int y) {
    get(y);
  }

  public static void get(@org.checkerframework.checker.index.qual.NonNegative int x) {}
}
