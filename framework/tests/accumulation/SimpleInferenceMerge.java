import org.checkerframework.framework.testchecker.testaccumulation.qual.*;

public class SimpleInferenceMerge {
  void build(@TestAccumulation({"a", "b"}) SimpleInferenceMerge this) {}

  void a() {}

  void b() {}

  void c() {}

  static void doStuffCorrectMerge(boolean b) {
    SimpleInferenceMerge s = new SimpleInferenceMerge();
    if (b) {
      s.a();
      s.b();
    } else {
      s.b();
      s.a();
      s.c();
    }
    s.build();
  }

  static void doStuffWrongMerge(boolean b) {
    SimpleInferenceMerge s = new SimpleInferenceMerge();
    if (b) {
      s.a();
      s.b();
    } else {
      s.b();
      s.c();
    }
    // :: error: method.invocation
    s.build();
  }
}
