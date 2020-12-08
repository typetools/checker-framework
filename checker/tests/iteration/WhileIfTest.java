package iteration;

import java.util.Iterator;

public class WhileIfTest {
    void test(Iterator<Integer> itera, Iterator<Integer> iterb) {
        while (itera.hasNext() && iterb.hasNext()) {
            itera.next();
            iterb.next();
        }
    }
}
