import java.util.Collection;

// Test case to show fix for https://github.com/typetools/checker-framework/issues/7217
class Issue7217<E extends Enum<E>> {

  void test(boolean b, Collection<E> c1, Collection<E> c2) {
    Object x = b ? c1 : c2;
  }
}
