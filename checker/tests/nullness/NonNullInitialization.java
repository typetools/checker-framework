import org.checkerframework.checker.nullness.qual.*;

// :: error: (initialization.fields.uninitialized)
public class NonNullInitialization {
    private String test;

    public static void main(String[] args) {
        NonNullInitialization n = new NonNullInitialization();
        n.test.equals("ASD");
    }
}
