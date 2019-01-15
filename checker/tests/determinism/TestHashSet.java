package determinism;

import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

class TestHashSet {
    static void testConstruct() {
        // :: error: (assignment.type.incompatible)
        @Det Set<String> s = new HashSet<String>();
    }

    static void testConstructCollection(@Det List<String> collection) {
        // :: error: (argument.type.incompatible)
        System.out.println(new HashSet<String>(collection));
    }

    static void testDetInvalid() {
        // :: error: (assignment.type.incompatible)
        @Det Set<String> s = new @Det HashSet<String>();
    }

    static void testIteration() {
        Set<String> s = new HashSet<String>();
        for (String str : s) {
            // :: error: (argument.type.incompatible)
            System.out.println(str);
        }
    }
}
