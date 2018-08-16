import org.checkerframework.checker.determinism.qual.*;

public class TestStaticMethod {
    static int test() {
        return 0;
    }

    static @NonDet int test1() {
        return 0;
    }

    void caller() {
        System.out.println(TestStaticMethod.test());
        // :: error: (argument.type.incompatible)
        System.out.println(TestStaticMethod.test1());
    }
}
