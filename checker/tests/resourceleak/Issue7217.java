import java.util.Collection;

class Issue7217<E extends Enum<E>> {

  /**
   * This method triggers a Least Upper Bound (LUB) merge of two collections of a captured type
   * variable E. * Before the fix, the ternary operator causes the Resource Leak Checker to crash
   * with a NullPointerException in CFAbstractValue.
   */
  void test(boolean b, Collection<E> c1, Collection<E> c2) {
    Object x = b ? c1 : c2;
  }
}
