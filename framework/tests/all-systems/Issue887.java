// Test case for Issue 887
// https://github.com/typetools/checker-framework/issues/887
// Additional test case in checker/tests/nullness/Issue887.java
import java.util.List;

@SuppressWarnings(
        "list.access.unsafe.high") // The Index Checker correctly issues this warning here.
public abstract class Issue887 {
    @SuppressWarnings("nullness") // See checker/tests/nullness/Issue887.java
    void test() {
        method(foo(null).get(0));
    }

    void method(Number o) {}

    abstract <T extends Number> List<? extends T> foo(T t);
}
