import org.checkerframework.framework.testchecker.testaccumulation.qual.*;

public class SimpleInference {
  void build(@TestAccumulation({"a"}) SimpleInference this) {}

  void doublebuild(@TestAccumulation({"a", "b"}) SimpleInference this) {}

  void a() {}

  void b() {}

  static void doStuffCorrect() {
    SimpleInference s = new SimpleInference();
    s.a();
    s.build();
  }

  static void doStuffCorrect2() {
    SimpleInference s = new SimpleInference();
    s.a();
    s.b();
    s.doublebuild();
  }

  static void doStuffWrong() {
    SimpleInference s = new SimpleInference();
    // :: error: method.invocation
    s.build();
  }

  static void doStuffWrong2() {
    SimpleInference s = new SimpleInference();
    s.a();
    // :: error: method.invocation
    s.doublebuild();
  }
}
