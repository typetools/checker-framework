import testlib.lib.UncheckedByteCode;

public class TestUncheckedByteCode {
    Object field;

    void test(UncheckedByteCode<Object> param, Integer i) {
        field = param.getCT();
        field = param.getInt(1);
        // Signedness Checker doesn't default boxed primitives correctly.
        // https://github.com/typetools/checker-framework/issues/797
        // :: error: (argument.type.incompatible)
        field = param.getInteger(i);
        field = param.getObject(new Object());
        field = param.getString("hello");
        field = param.identity("hello");
    }
}
