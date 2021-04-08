// Test case for Issue 1920:
// https://github.com/typetools/checker-framework/issues/1920

import java.util.Iterator;
import java.util.NoSuchElementException;

@SuppressWarnings("all") // Only check for crashes
public class Issue1920 {
  static class Foo implements Iterable {
    public Iterator iterator() {
      return new Iterator() {
        @Override
        public boolean hasNext() {
          return false;
        }

        @Override
        public Object next() {
          throw new NoSuchElementException();
        }
      };
    }
  }

  static void testErasedIterator(Foo foo) {
    for (Object x : foo) {
      x.hashCode();
    }
  }
}
