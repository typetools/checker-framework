// Test case for Issue1111
// https://github.com/typetools/checker-framework/issues/1111
// Additional test case in checker/tests/tainting/Issue1111.java

import java.util.List;

@SuppressWarnings("") // just check for crash
public class Issue1111 {
    void foo(Box<? super Integer> box, List<Integer> list) {
        bar(box, list);
    }

    <T extends Number> void bar(Box<T> box, Iterable<? extends T> list) {}

    class Box<T extends Number> {}
}
