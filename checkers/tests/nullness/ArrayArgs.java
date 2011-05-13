import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("Nullable")
public class ArrayArgs {

    public void test(@NonNull String[] args) {
    }

    public void test(Class<? extends java.lang.annotation.Annotation> cls) {
    }

    public void test() {
        test(NonNull.class);

        String[] s1 = new String[] { null, null, null };
        //:: error: (argument.type.incompatible)
        test(s1);
        String[] s2 = new String[] { "hello", null, "goodbye" };
        //:: error: (argument.type.incompatible)
        test(s2);
        //:: error: (assignment.type.incompatible)
        @NonNull String[] s3 = new String[] { "hello", null, "goodbye" };
        //:: error: (assignment.type.incompatible)
        @NonNull String[] s4 = new String[3];

        // TODO:  uncomment when issue 25 is fixed
        // String[] s5 = new String[] { "hello", "goodbye" };
        // test(s5);
        // @NonNull String[] s6 = new String[] { "hello", "goodbye" };
        // test(s6);
    }
}
