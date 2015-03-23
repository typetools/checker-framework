// Test case for Issue 312:
// https://code.google.com/p/checker-framework/issues/detail?id=312

import java.util.List;

// A mock-up of the Guava API used in the test case;
// see below for the code that uses Guava.
@SuppressWarnings("nullness")
class Ordering<T> {
    public static <C extends Comparable> Ordering<C> natural() {
        return null;
    }

    public <S extends T> Ordering<S> reverse() {
        return null;
    }

    public <E extends T> List<E> sortedCopy(Iterable<E> elements) {
        return null;
    }
}

class Issue312 {
    void test(List<String> list) {
        Ordering.natural().reverse().sortedCopy(list);
    }
}

/* Original test using Guava:

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import java.util.List;

class Issue312 {
    void test() {
        List<String> list = Lists.newArrayList();
        Ordering.natural().reverse().sortedCopy(list);
    }
}

*/
