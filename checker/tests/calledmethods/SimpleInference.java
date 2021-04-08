import org.checkerframework.checker.calledmethods.qual.*;

/* The simplest inference test case Martin could think of */
public class SimpleInference {
  void build(@CalledMethods({"a"}) SimpleInference this) {}

  void a() {}

  static void doStuffCorrect() {
    SimpleInference s = new SimpleInference();
    s.a();
    s.build();
  }

  static void doStuffWrong() {
    SimpleInference s = new SimpleInference();
    // :: error: finalizer.invocation.invalid
    s.build();
  }
}
