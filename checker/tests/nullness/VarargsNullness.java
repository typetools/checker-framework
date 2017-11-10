import org.checkerframework.checker.nullness.qual.*;

public class VarargsNullness {

    public void test(@NonNull Object @NonNull ... o) {
        for (@NonNull Object p : o) {
            System.out.println(p);
        }
    }

    public void test2(Object o1, Object o2) {
        System.out.println(o1);
        System.out.println(o2);
    }

    public void testVarargs() {
        test("foo", "bar", "baz");
    }

    public void testVarargsNoArgs() {
        test();
    }

    public void testNonVarargs() {
        test2("foo", "bar");
    }

    public void format1(java.lang.String a1, java.lang.@Nullable Object... a2) {
        int x = a2.length; // no warning
        // :: error: (enhancedfor.type.incompatible)
        for (@NonNull Object p : a2) // warning
        System.out.println(p);
    }

    public void format2(java.lang.String a1, java.lang.Object @Nullable ... a2) {
        // :: error: (dereference.of.nullable)
        int x = a2.length; // warning
        for (@NonNull Object p : a2) // no warning
        System.out.println(p);
    }

    public void testPrintf() {
        String s = null;
        printf("%s", s);
        // tests do not use annotated jdk
        // System.out.printf ("%s", s);
    }

    // printf declaration is taken from PrintStream
    public java.io.PrintStream printf(java.lang.String a1, java.lang.@Nullable Object... a2) {
        throw new RuntimeException("skeleton method");
    }
}
