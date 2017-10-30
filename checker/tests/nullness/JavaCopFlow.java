import org.checkerframework.checker.nullness.qual.*;

@org.checkerframework.framework.qual.DefaultQualifier(Nullable.class)
class JavaCopFlow {

    public void testIf(String str) {

        // String str = "foo";
        @NonNull String a;
        if (str != null) {
            a = str;
        }

        str = null;
        // :: error: (assignment.type.incompatible)
        @NonNull String b = str;
    }

    public void testIfNoBlock(String str) {

        // String str = "foo";
        @NonNull String a;
        if (str != null) {
            a = str;
        }

        str = null;
        // :: error: (assignment.type.incompatible)
        @NonNull String b = str;
    }

    public void testElse(String str) {

        // String str = "foo";
        @NonNull String a;
        if (str == null) {
            testAssert("");
        } else {
            a = str;
        }

        str = null;
        // :: error: (assignment.type.incompatible)
        @NonNull String b = str;
    }

    public void testElseNoBlock(String str) {

        // String str = "foo";
        @NonNull String a;
        if (str == null) {
            testAssert("");
        } else {
            a = str;
        }

        str = null;
        // :: error: (assignment.type.incompatible)
        @NonNull String b = str;
    }

    public void testReturnIf(String str) {

        // String str = "foo";
        if (str == null) {
            testAssert("");
            return;
        }

        @NonNull String a = str;

        str = null;
        // :: error: (assignment.type.incompatible)
        @NonNull String b = str;
    }

    public void testReturnElse(String str) {

        //        String str = "foo";
        if (str != null) {
            testAssert("");
        } else {
            return;
        }

        @NonNull String a = str;

        str = null;
        // :: error: (assignment.type.incompatible)
        @NonNull String b = str;
    }

    public void testThrowIf(String str) {

        // String str = "foo";
        if (str == null) {
            testAssert("");
            throw new RuntimeException("foo");
        }

        @NonNull String a = str;

        str = null;
        // :: error: (assignment.type.incompatible)
        @NonNull String b = str;
    }

    public void testThrowElse(String str) {

        // String str = "foo";
        if (str != null) {
            testAssert("");
        } else {
            throw new RuntimeException("foo");
        }

        @NonNull String a = str;

        str = null;
        // :: error: (assignment.type.incompatible)
        @NonNull String b = str;
    }

    public void testAssert(@Nullable String str) {

        assert str != null : "@AssumeAssertion(nullness)";

        @NonNull String a = str;

        str = null;
        // :: error: (assignment.type.incompatible)
        @NonNull String b = str;
    }

    public void testWhile(String str) {

        // String str = "foo";
        while (str != null) {
            @NonNull String a = str;
            break;
        }

        str = null;
        // :: error: (assignment.type.incompatible)
        @NonNull String b = str;
    }

    public void testIfInstanceOf(String str) {

        // String str = "foo";
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
    }

    public void testExit(String str) {

        // String str = null;
        if (str == null) {
            System.exit(0);
        }

        @NonNull String a = str;
    }

    void methodThatThrowsRuntime() {
        throw new RuntimeException();
    }

    public void retestWhile(@Nullable String str) {

        while (str != null) {
            @NonNull String a = str;
            break;
        }

        int i = 0;
        while (true) {
            // :: error: (assignment.type.incompatible)
            @NonNull String a = str;
            str = null;
            i++;
            if (i > 2) break;
        }

        str = null;
        @NonNull String b = "hi";
        try {
            // :: error: (assignment.type.incompatible)
            b = str;
            methodThatThrowsRuntime();
            str = "bar";
        } finally {
            // :: error: (assignment.type.incompatible)
            b = str;
        }

        str = null;
        // :: error: (assignment.type.incompatible)
        b = str;

        str = "hi";
        b = (String) str;
    }
}
