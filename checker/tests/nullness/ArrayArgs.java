import org.checkerframework.checker.nullness.qual.*;

@org.checkerframework.framework.qual.DefaultQualifier(Nullable.class)
public class ArrayArgs {

    public void test(@NonNull String[] args) {}

    public void test(Class<@NonNull ? extends java.lang.annotation.Annotation> cls) {}

    public void test() {
        test(NonNull.class);

        String[] s1 = new String[] {null, null, null};
        // :: error: (argument.type.incompatible)
        test(s1);
        String[] s2 = new String[] {"hello", null, "goodbye"};
        // :: error: (argument.type.incompatible)
        test(s2);
        // :: error: (assignment.type.incompatible)
        @NonNull String[] s3 = new String[] {"hello", null, "goodbye"};
        // :: error: (new.array.type.invalid)
        @NonNull String[] s4 = new String[3];

        // TODO: when issue 25 is fixed, the following is safe
        // and no error needs to be raised.
        String[] s5 = new String[] {"hello", "goodbye"};
        // :: error: (argument.type.incompatible)
        test(s5);
        @NonNull String[] s6 = new String[] {"hello", "goodbye"};
        test(s6);
    }
}
