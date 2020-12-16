import org.checkerframework.framework.testchecker.util.*;

public class Basic {

    @Odd String field;

    void test(@Odd String param) {
        String local = "";
        local = param;
        field = local;

        String r = field;
    }

    void testIf(@Odd String ifParam) {
        String local = "";
        if (field != null) {
            local = ifParam;
        } else {
            local = ifParam;
        }

        String r = local;
    }

    void testWhile(@Odd String whileParam) {
        String local = whileParam;
        while (local != "foo") {
            local = "";
        }

        String r = local;
    }

    void testWhile2(@Odd String whileParam) {
        String local = "";
        while (local != "foo") {
            local = whileParam;
        }

        String r = local;
    }

    void testCompountAssignment(@Odd String odd) {
        String nonOdd = odd;
        nonOdd += "kj"; // nonOdd as rValue is not Odd necessarily!
        nonOdd = "m";
    }
}
