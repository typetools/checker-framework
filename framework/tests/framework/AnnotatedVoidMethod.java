import testlib.util.*;

public class AnnotatedVoidMethod {
    // :: error: annotation type not applicable to this kind of declaration
    public @Odd void method() {
        return;
    }
}
