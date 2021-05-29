import org.checkerframework.framework.testchecker.lib.UncheckedByteCode;

public class TestUncheckedByteCode {
  Object field;

  void test(UncheckedByteCode<Object> param, Integer i) {
    field = param.getCT();
    // :: error: (argument)
    field = param.getInt(1);
    // Signedness Checker doesn't default boxed primitives correctly.
    // https://github.com/typetools/checker-framework/issues/797
    // :: error: (argument)
    field = param.getInteger(i);
    // :: error: (argument)
    field = param.getObject(new Object());
    // :: error: (argument)
    field = param.getString("hello");
    field = param.identity("hello");
  }
}
