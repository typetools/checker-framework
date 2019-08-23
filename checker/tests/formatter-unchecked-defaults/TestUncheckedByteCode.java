import testlib.lib.UncheckedByteCode;

public class TestUncheckedByteCode {
    Object field;

    void test(UncheckedByteCode<Object> param, Integer i) {
        field = param.getCT();
        field = param.getInt(1);
        field = param.getInteger(i);
        field = param.getObject(new Object());
        field = param.getString("hello");
        field = param.identity("hello");
    }
}
