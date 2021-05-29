import org.checkerframework.checker.formatter.qual.FormatMethod;

public class InvalidFormatMethod {

  @FormatMethod
  void m1(String s, Object... args) {}

  @FormatMethod
  void m2(int x, double y, boolean z, String s, Object... args) {}

  @FormatMethod
  // :: error: (format.method)
  void m3(int x, Object... args) {}

  void client(Object... args) {
    m1("hello");
    m1("hello %s", "goodbye");
    m2(22, 3.14, true, "hello");
    m2(22, 3.14, true, "hello %s", "goodbye");
    // :: error: (format.method)
    m3(22, "goodbye");
  }
}
