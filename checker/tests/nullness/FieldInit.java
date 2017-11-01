import org.checkerframework.checker.nullness.qual.*;

public class FieldInit {
    // :: error: (argument.type.incompatible) :: error: (method.invocation.invalid)
    String f = init(this);

    String init(FieldInit o) {
        return "";
    }

    void test() {
        String local = init(this);
    }
}
