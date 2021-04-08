public class CheckNotNull2<T extends Object> {
  T checkNotNull(T ref) {
    return ref;
  }

  void test(T ref) {
    checkNotNull(ref);
  }
}
