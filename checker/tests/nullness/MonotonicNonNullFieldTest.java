// Testcase for Issue553
// https://github.com/typetools/checker-framework/issues/553
import org.checkerframework.checker.nullness.qual.*;

public class MonotonicNonNullFieldTest {
    class Data {
        @MonotonicNonNull Object field;
    }

    void method(Object object) {}

    @RequiresNonNull("#1.field")
    void test(final Data data) {
        method(data.field); // checks OK

        Runnable callback =
                new Runnable() {
                    public void run() {
                        method(data.field); // used to issue error
                    }
                };
    }
}
