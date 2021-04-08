import org.checkerframework.checker.index.qual.*;

public class ArrayIntroWithCast<T> {

  void test(String[] a, String[] b) {
    Object result = new Object[a.length + b.length];
    System.arraycopy(a, 0, result, 0, a.length);
  }

  void test2(String[] a, String[] b) {
    @SuppressWarnings("unchecked")
    T[] result = (T[]) new Object[a.length + b.length];
    System.arraycopy(a, 0, result, 0, a.length);
  }
}
