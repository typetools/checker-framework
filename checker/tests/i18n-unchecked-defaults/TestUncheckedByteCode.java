import org.checkerframework.framework.testchecker.lib.UncheckedByteCode;

public class TestUncheckedByteCode {
    Object field;

    void test(UncheckedByteCode<Object> param, Integer i) {
        field = param.getCT();
        field = param.getInt(1);
        field = param.getInteger(i);
        field = param.identity("hello");

        // String and Object are relevant types and must be annotated in bytecode
        // :: error: (argument.type.incompatible)
        field = param.getObject(new Object());
        // :: error: (argument.type.incompatible)
        field = param.getString("hello");
    }
}
