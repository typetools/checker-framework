// Test case for Issue 1981:
// https://github.com/typetools/checker-framework/issues/1981

import java.util.List;

class Issue1981 {

    @SuppressWarnings("unchecked")
    void test(List ids) {
        for (List l : func2(func1(ids))) {}
    }

    static <E extends Comparable<? super E>> List func1(Iterable<? extends E> elements) {
        // :: error: (return.type.incompatible)
        return null;
    }

    static List<List> func2(List list) {
        // :: error: (return.type.incompatible)
        return null;
    }
}
