import org.checkerframework.checker.calledmethods.qual.*;

/* The simplest inference test case Martin could think of */
public class SimpleInferenceMerge {
  void build(@CalledMethods({"a", "b"}) SimpleInferenceMerge this) {}

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
    // :: error: finalizer.invocation
    s.build();
  }
}
