import org.checkerframework.checker.nullness.qual.*;

class SuppressWarningsTest {

    @SuppressWarnings("all")
    void test() {
        String a = null;
        a.toString();
    }
}
