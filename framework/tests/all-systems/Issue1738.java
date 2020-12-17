// Test case for Issue 1738:
// https://github.com/typetools/checker-framework/issues/1738

import java.util.Iterator;

@SuppressWarnings("all") // Only check for crashes
public class Issue1738 {
    static class TwoParamIterator<T, R> implements Iterator<T> {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public T next() {
            return null;
        }
    }

    static class TwoParamCollection<T, R> implements Iterable<T> {
        @Override
        public TwoParamIterator<T, R> iterator() {
            return new TwoParamIterator<T, R>();
        }
    }

    static void test() {
        TwoParamCollection<String, String> c = new TwoParamCollection<>();
        for (String s : c) {
            s.hashCode();
        }
    }
}
