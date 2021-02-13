import org.checkerframework.checker.nullness.qual.*;

public class FlowNullness {

    public void testIf() {

        String str = "foo";
        @NonNull String a;
        // :: warning: (nulltest.redundant)
        if (str != null) {
            a = str;
        }

        str = null;
        // :: error: (assignment.type.incompatible)
        @NonNull String b = str;
    }

    public void testIfNoBlock() {

        String str = "foo";
        @NonNull String a;
        // :: warning: (nulltest.redundant)
        if (str != null) {
            a = str;
        }

        str = null;
        // :: error: (assignment.type.incompatible)
        @NonNull String b = str;
    }

    public void testElse() {

        String str = "foo";
        @NonNull String a;
        // :: warning: (nulltest.redundant)
        if (str == null) {
            testAssert();
        } else {
            a = str;
        }

        str = null;
        // :: error: (assignment.type.incompatible)
        @NonNull String b = str;
    }

    public void testElseNoBlock() {

        String str = "foo";
        @NonNull String a;
        // :: warning: (nulltest.redundant)
        if (str == null) {
            testAssert();
        } else {
            a = str;
        }

        str = null;
        // :: error: (assignment.type.incompatible)
        @NonNull String b = str;
    }

    public void testReturnIf() {

        String str = "foo";
        // :: warning: (nulltest.redundant)
        if (str == null) {
            testAssert();
            return;
        }

        @NonNull String a = str;

        str = null;
        // :: error: (assignment.type.incompatible)
        @NonNull String b = str;
    }

    public void testReturnElse() {

        String str = "foo";
        // :: warning: (nulltest.redundant)
        if (str != null) {
            testAssert();
        } else {
            return;
        }

        @NonNull String a = str;

        str = null;
        // :: error: (assignment.type.incompatible)
        @NonNull String b = str;
    }

    public void testThrowIf() {

        String str = "foo";
        // :: warning: (nulltest.redundant)
        if (str == null) {
            testAssert();
            throw new RuntimeException("foo");
        }

        @NonNull String a = str;

        str = null;
        // :: error: (assignment.type.incompatible)
        @NonNull String b = str;
    }

    public void testThrowElse() {

        String str = "foo";
        // :: warning: (nulltest.redundant)
        if (str != null) {
            testAssert();
        } else {
            throw new RuntimeException("foo");
        }

        @NonNull String a = str;

        str = null;
        // :: error: (assignment.type.incompatible)
        @NonNull String b = str;
    }

    public void testAssert() {

        String str = "foo";
        // :: warning: (nulltest.redundant)
        assert str != null;

        @NonNull String a = str;

        str = null;
        // :: error: (assignment.type.incompatible)
        @NonNull String b = str;
    }

    public void testWhile() {

        String str = "foo";
        // :: warning: (nulltest.redundant)
        while (str != null) {
            @NonNull String a = str;
            break;
        }

        str = null;
        // :: error: (assignment.type.incompatible)
        @NonNull String b = str;
    }

    public void testIfInstanceOf() {

        String str = "foo";
        @NonNull String a;
        if (str instanceof String) {
            a = str;
        }

        str = null;
        // :: error: (assignment.type.incompatible)
        @NonNull String b = str;
    }

    public void testNew() {

        String str = "foo";
        @NonNull String a = str;

        str = null;
        // :: error: (assignment.type.incompatible)
        @NonNull String b = str;

        String s2 = new String();
        s2.toString();
    }

    public void testExit() {

        String str = "foo";
        // :: warning: (nulltest.redundant)
        if (str == null) {
            System.exit(0);
        }

        @NonNull String a = str;
    }

    void testMore() {
        String str = null + " foo";
        @NonNull String a = str;
    }

    void orderOfEvaluation() {
        class MyClass {
            @org.checkerframework.dataflow.qual.Pure
            public boolean equals(@Nullable Object o) {
                return o != null;
            }

            void test(@Nullable Object a, @Nullable Object b) {}
        }
        MyClass m = new MyClass();
        m.equals(m = null);

        MyClass n = new MyClass();
        // :: error: (dereference.of.nullable)
        n.test(n = null, n.toString()); // error

        MyClass o = null;
        // :: error: (dereference.of.nullable)
        o.equals(o == new MyClass()); // error
    }

    void instanceOf(@Nullable Object o) {
        if (o instanceof String) {
            // cannot be null here
            o.toString();
            return;
        }
        // :: error: (dereference.of.nullable)
        o.toString(); // error
    }

    public static void checkConditional1(@Nullable Object a) {
        if (a == null) {
        } else {
            a.getClass(); // not an error
        }
    }

    public static void checkConditional2(@Nullable Object a) {
        if (a == null) {
        } else if (a instanceof String) {
        } else {
            a.getClass(); // not an error
        }
    }

    public static String spf(String format, @NonNull Object[] args) {
        int current_arg = 0;
        Object arg = args[current_arg];
        if (false) {
            return arg.toString(); // not an error
        }
        if (arg instanceof long[]) {
            return "foo";
        } else {
            return arg.toString(); // still not an error
        }
    }

    void empty_makes_no_change() {
        String o1 = "not null!";
        if (false) {
            // empty branch
        } else {
            o1 = "still not null!";
        }
        System.out.println(o1.toString());
    }

    @org.checkerframework.dataflow.qual.Pure
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof Integer)) {
            return false;
        }
        @NonNull Object nno = o;
        @NonNull Integer nni = (Integer) o;
        return true;
    }

    void while_set_and_test(@Nullable String s) {
        String line;
        // imagine "s" is "reader.readLine()" (but avoid use of libraries
        // in unit tests)
        while ((line = s) != null) {
            line.trim();
        }
    }

    void equality_test(@Nullable String s) {
        @NonNull String n = "m";
        if (s == n) {
            s.toString();
        }
    }

    @Nullable Object returnNullable() {
        return null;
    }

    void testNullableCall() {
        if (returnNullable() != null) {
            // :: error: (dereference.of.nullable)
            returnNullable().toString(); // error
        }
    }

    void nonNullArg(@NonNull Object arg) {
        // empty body
    }

    void testNonNullArg(@Nullable Object arg) {
        // :: error: (argument.type.incompatible)
        nonNullArg(arg); // error
        nonNullArg(arg); // no error
    }

    void test() {
        String[] s = null;
        // :: error: (dereference.of.nullable)
        for (int i = 0; i < s.length; ++i) { // error
            String m = s[i]; // fine.. s cannot be null
        }
    }

    private double @MonotonicNonNull []
            intersect; // = null; TODO: do we want to allow assignments of null to MonotonicNonNull?

    public void add_modified(double[] a, int count) {
        // System.out.println ("common: " + ArraysMDE.toString (a));
        // :: warning: (nulltest.redundant)
        if (a == null) {
            return;
        } else if (intersect == null) {
            intersect = a;
            return;
        }

        double[] tmp = new double[intersect.length];
    }
}
