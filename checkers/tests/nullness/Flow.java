import checkers.nullness.quals.*;

public class Flow {

    public void testIf() {

        String str = "foo";
        @NonNull String a;
        if (str != null) {
            a = str;
        }

        str = null;
        @NonNull String b = str;
    }

    public void testIfNoBlock() {

        String str = "foo";
        @NonNull String a;
        if (str != null)
            a = str;

        str = null;
        @NonNull String b = str;
    }

    public void testElse() {

        String str = "foo";
        @NonNull String a;
        if (str == null) {
            testAssert();
        } else {
            a = str;
        }

        str = null;
        @NonNull String b = str;
    }

    public void testElseNoBlock() {

        String str = "foo";
        @NonNull String a;
        if (str == null)
            testAssert();
        else
            a = str;

        str = null;
        @NonNull String b = str;
    }

    public void testReturnIf() {

        String str = "foo";
        if (str == null) {
            testAssert();
            return;
        }

        @NonNull String a = str;

        str = null;
        @NonNull String b = str;
    }

    public void testReturnElse() {

        String str = "foo";
        if (str != null) {
            testAssert();
        } else
            return;

        @NonNull String a = str;

        str = null;
        @NonNull String b = str;
    }

    public void testThrowIf() {

        String str = "foo";
        if (str == null) {
            testAssert();
            throw new RuntimeException("foo");
        }

        @NonNull String a = str;

        str = null;
        @NonNull String b = str;
    }

    public void testThrowElse() {

        String str = "foo";
        if (str != null) {
            testAssert();
        } else
            throw new RuntimeException("foo");

        @NonNull String a = str;

        str = null;
        @NonNull String b = str;
    }

    public void testAssert() {

        String str = "foo";
        assert str != null;

        @NonNull String a = str;

        str = null;
        @NonNull String b = str;
    }

    public void testWhile() {

        String str = "foo";
        while (str != null) {
            @NonNull String a = str;
            break;
        }

        str = null;
        @NonNull String b = str;
    }

    public void testIfInstanceOf() {

        String str = "foo";
        @NonNull String a;
        if (str instanceof String) {
            a = str;
        }

        str = null;
        @NonNull String b = str;
    }

    public void testNew() {

        String str = "foo";
        @NonNull String a = str;

        str = null;
        @NonNull String b = str;

        String s2 = new String();
        s2.toString();
    }

    public void testExit() {

        String str = "foo";
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
            public boolean equals(@Nullable Object o) { return true; }
            void test(@Nullable Object a, @Nullable Object b) { }
        }
        MyClass m = new MyClass();
        m.equals(m = null);

        MyClass n = new MyClass();
        n.test(n = null, n.toString()); // error

        MyClass o = null;
        o.equals(o == new MyClass());   // error
    }

    void instanceOf(@Nullable Object o) {
        if (o instanceof String) {
            // cannot be null here
            o.toString();
            return;
        }
        o.toString(); // error
    }

    public static void checkConditional1(@Nullable Object a) {
        if (a == null) {
        } else {
            a.getClass();         // not an error
        }
    }

    public static void checkConditional2(@Nullable Object a) {
        if (a == null) {
        } else if (a instanceof String) {
        } else {
            a.getClass();         // not an error
        }
    }

    public static String spf (String format, @NonNull Object[] args) {
        int current_arg = 0;
        Object arg = args[current_arg];
        if (false)
            return arg.toString(); // not an error
        if (arg instanceof long[])
            return "foo";
        else
            return arg.toString(); // still not an error
    }

    void empty_makes_no_change() {
        @Nullable String o1 = "not null!";
        if (false) {
            // empty branch
        } else {
            o1 = "still not null!";
        }
        System.out.println(o1.toString());
    }

    void type_refining() {
        @Nullable String o1 = "not null!";
        o1.toString();
    }

    public boolean equals(@Nullable Object o) {
        if (!(o instanceof Integer))
            return false;
        @NonNull Object nno = o;
        @NonNull Integer nni = (Integer)o;
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
        if (returnNullable() != null)
            returnNullable().toString();
    }

    // This might be useful: "if this routine has ever returned non-null
    // before, it will never return null again".  But hold off until there
    // is a demostrated need.
    // @LazyNonNull Object returnLazyNonNull() {
    //     return null;
    // }
    // void testLazyNonNullCall() {
    //     if (returnLazyNonNull() != null)
    //         returnLazyNonNull().toString();
    // }

}
