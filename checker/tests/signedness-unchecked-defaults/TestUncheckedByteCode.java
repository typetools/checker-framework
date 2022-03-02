import org.checkerframework.checker.signedness.qual.UnknownSignedness;
import org.checkerframework.framework.testchecker.lib.UncheckedByteCode;

public class TestUncheckedByteCode {
    @UnknownSignedness Object field;

    void test(UncheckedByteCode<Object> param, Integer i) {
        field = param.getCT();
        // :: error: (argument.type.incompatible)
        field = param.getInt(1);
        // Signedness Checker doesn't default boxed primitives correctly.
        // https://github.com/typetools/checker-framework/issues/797
        // :: error: (argument.type.incompatible)
        field = param.getInteger(i);
        // :: error: (argument.type.incompatible)
        field = param.getObject(new Object());
        // :: error: (argument.type.incompatible)
        field = param.getString("hello");
        field = param.identity("hello");
    }
}
