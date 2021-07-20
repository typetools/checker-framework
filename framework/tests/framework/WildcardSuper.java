import org.checkerframework.framework.testchecker.util.*;

import java.util.List;

public class WildcardSuper {
    void test(List<? super @Odd String> list) {
        // :: error: (assignment.type.incompatible)
        @Odd Object odd = list.get(0);
    }
}
