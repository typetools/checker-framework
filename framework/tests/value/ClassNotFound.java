import org.checkerframework.common.value.qual.*;
import org.checkerframework.dataflow.qual.Pure;

public class ClassNotFound {

    @StaticallyExecutable
    @Pure
    public static int foo(int a) {
        return a + 2;
    }

    public void bar() {
        int a = 0;
        // :: warning: (class.find.failed)
        foo(a);
    }
}
