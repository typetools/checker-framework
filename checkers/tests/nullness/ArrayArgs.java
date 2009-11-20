import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.Nullable")
public class ArrayArgs {

    public void test(@NonNull String[] args) {
    }

    public void test(Class<? extends java.lang.annotation.Annotation> cls) {
    }

    public void test() {
        String[] s = new String[] { null, null, null };
        test(s);
        test(NonNull.class);
    }
}
