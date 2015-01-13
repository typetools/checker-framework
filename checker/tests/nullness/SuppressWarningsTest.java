import org.checkerframework.checker.nullness.qual.*;

import java.lang.SuppressWarnings;

class SuppressWarningsTest {

    @SuppressWarnings("all")
    void test() {
        String a = null;
        a.toString();
    }
}
