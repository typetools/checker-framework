import org.checkerframework.framework.testchecker.util.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TypeInference {

    void test() {
        Collection<@Odd String> lst1 = Collections.<@Odd String>emptyList();
        // :: error: (assignment.type.incompatible)
        Collection<@Odd String> lst2 = Collections.<String>emptyList(); // should emit error
        // :: error: (assignment.type.incompatible)
        Collection<String> lst3 = Collections.<@Odd String>emptyList(); // should emit error
        Collection<@Odd String> lst4 = Collections.emptyList();
        Map<Integer, @Odd String> lst5 = Collections.emptyMap();
        Map<Integer, String> lst6 = Collections.emptyMap();
    }

    static class MyMap<E> extends HashMap<String, E> {}

    static <T> MyMap<T> getMap() {
        return null;
    }

    void testSuper() {
        MyMap<@Odd String> m1 = TypeInference.<@Odd String>getMap();
        MyMap<@Odd String> m2 = getMap();
        // :: error: (assignment.type.incompatible)
        MyMap<String> m3 = TypeInference.<@Odd String>getMap(); // should emit error
        MyMap<String> m4 = getMap();

        Map<String, @Odd Integer> m5 = getMap();
        Map<String, Integer> m6 = getMap();
    }
}
