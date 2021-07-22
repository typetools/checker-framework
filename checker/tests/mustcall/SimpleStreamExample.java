// Based on a false positive in Zookeeper

import java.util.*;

class SimpleStreamExample {
    static void test(List<SimpleStreamExample> s) {
        s.stream().filter(str -> str == null);
    }
}
