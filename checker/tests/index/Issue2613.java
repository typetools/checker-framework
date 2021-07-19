import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.LessThan;

public class Issue2613 {

    private static final String STRING_CONSTANT = "Hello";

    void integerConstant() {
        require_lt(0, Integer.MAX_VALUE);
    }

    void StringConstant() {
        require_lt(0, STRING_CONSTANT);
    }

    void require_lt(@LessThan("#2") int a, int b) {}

    void require_lt(@LTLengthOf("#2") int a, String b) {}

    void method(@LessThan("Integer.MAX_VALUE") long x, @LessThan("Integer.MAX_VALUE") long y) {
        x = y;
        @LessThan("2147483647") long z = y;
    }
}
