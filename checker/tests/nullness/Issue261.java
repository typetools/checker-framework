// Test case for Issue 261
// https://github.com/typetools/checker-framework/issues/261
public class Issue261 {
  boolean b;

  class Flag<T> {
    // :: error: (initialization.field.uninitialized)
    T value;
  }

  static <T> T getValue(Flag<T> flag) {
    return flag.value;
  }

  Issue261(Flag<Boolean> flag) {
    this.b = getValue(flag);
  }
}
