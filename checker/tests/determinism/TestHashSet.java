package determinism;

import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

class TestHashSet {
    public static void testConstruct() {
        // :: error: (assignment.type.incompatible)
        @Det Set<String> s = new HashSet<String>();
    }

    public static void testIteration() {
        Set<String> s = new HashSet<String>();
        s.add("a");
        s.add("b");
        for (String str : s) {
            // :: error: (argument.type.incompatible)
            System.out.println(str);
        }
    }
}
