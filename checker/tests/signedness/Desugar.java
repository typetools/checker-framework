import org.checkerframework.checker.signedness.qual.PolySigned;
import org.checkerframework.checker.signedness.qual.Signed;

import java.util.Map;

public class Desugar {

    void test(int x) {
        int i = getI();
        Integer box = i;
        @Signed Integer boxy = box;
        @Signed Integer box2 = method(box);
    }

    @PolySigned Integer method(@PolySigned Integer i) {
        return i;
    }

    @Signed int getI() {
        return 0;
    }

    void test2(Map<Integer, String> nonceMap, String nextInvo) {
        int invoNonce = calcNonce(nextInvo);
        Integer key = invoNonce;
        String enterInvo = nonceMap.get(key);
    }

    private @Signed int calcNonce(String invocation) {
        return 0;
    }
}
