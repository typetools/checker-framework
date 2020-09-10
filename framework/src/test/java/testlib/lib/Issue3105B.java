package testlib.lib;

public class Issue3105B {
    public static final String FIELD1 = "foo";

    public static final String FIELD2;

    static {
        FIELD2 = "bar";
    }

    public static final String FIELD3;

    static {
        FIELD3 = "baz";
    }
}
