// Test case for Issue 261
// https://code.google.com/p/checker-framework/issues/detail?id=261
class Issue261 {
  boolean b;

  class Flag<T> {
    T value;
  }

  static <T> T getValue(Flag<T> flag) {
    return flag.value;
  }

  Issue261(Flag<Boolean> flag) {
    this.b = getValue(flag);
  }
}
