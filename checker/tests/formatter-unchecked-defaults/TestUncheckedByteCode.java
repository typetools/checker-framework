import testlib.lib.UncheckedByteCode;

public class TestUncheckedByteCode {
    Object field;

    void test(UncheckedByteCode<Object> param, Integer i) {
        field = param.getCT();
        field = param.getInt(1);
        field = param.getInteger(i);
        field = param.getObject(new Object());
        // Strings are relevant and must be annotated in bytecode
        // ::error: (argument.type.incompatible)
        field = param.getString("hello");
        field = param.identity("hello");
    }
}
