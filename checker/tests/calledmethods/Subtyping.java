import org.checkerframework.checker.calledmethods.qual.*;

// basic subtyping checks
public class Subtyping {
  @CalledMethods({}) Object top_top(@CalledMethods({}) Object o) {
    return o;
  }

  @CalledMethods({}) Object top_a(@CalledMethods({"a"}) Object o) {
    return o;
  }

  @CalledMethods({}) Object top_ab(@CalledMethods({"a", "b"}) Object o) {
    return o;
  }

  @CalledMethods({}) Object top_bot(@CalledMethodsBottom Object o) {
    return o;
  }

  @CalledMethods({"a"}) Object a_top(@CalledMethods({}) Object o) {
    // :: error: return
    return o;
  }

  @CalledMethods({"a"}) Object a_a(@CalledMethods({"a"}) Object o) {
    return o;
  }

  @CalledMethods({"a"}) Object a_ab(@CalledMethods({"a", "b"}) Object o) {
    return o;
  }

  @CalledMethods({"a"}) Object a_bot(@CalledMethodsBottom Object o) {
    return o;
  }

  @CalledMethods({"a", "b"}) Object ab_top(@CalledMethods({}) Object o) {
    // :: error: return
    return o;
  }

  @CalledMethods({"a", "b"}) Object ab_a(@CalledMethods({"a"}) Object o) {
    // :: error: return
    return o;
  }

  @CalledMethods({"a", "b"}) Object ab_ab(@CalledMethods({"a", "b"}) Object o) {
    return o;
  }

  @CalledMethods({"a", "b"}) Object ab_bot(@CalledMethodsBottom Object o) {
    return o;
  }

  @CalledMethodsBottom Object bot_top(@CalledMethods({}) Object o) {
    // :: error: return
    return o;
  }

  @CalledMethodsBottom Object bot_a(@CalledMethods({"a"}) Object o) {
    // :: error: return
    return o;
  }

  @CalledMethodsBottom Object bot_ab(@CalledMethods({"a", "b"}) Object o) {
    // :: error: return
    return o;
  }

  @CalledMethodsBottom Object bot_bot(@CalledMethodsBottom Object o) {
    return o;
  }
}
