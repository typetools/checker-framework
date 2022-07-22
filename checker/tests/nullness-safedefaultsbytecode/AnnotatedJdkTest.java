import org.checkerframework.checker.nullness.qual.*;

import java.util.HashMap;
import java.util.Set;

// There should be no warnings for the following operations
// if the annotated JDK is loaded properly
public class AnnotatedJdkTest {
    String toStringTest(Object v) {
        return v.toString();
    }

    Set<@KeyFor("#1") String> keySetTest(HashMap<String, Object> map) {
        return map.keySet();
    }
}
