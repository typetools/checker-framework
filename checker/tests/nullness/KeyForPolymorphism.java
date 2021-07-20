import org.checkerframework.checker.nullness.qual.*;

import java.util.HashMap;
import java.util.Map;

// test related to issue 429: https://github.com/typetools/checker-framework/issues/429
public class KeyForPolymorphism {

    Map<String, Object> m1 = new HashMap<>();
    Map<String, Object> m2 = new HashMap<>();

    void method(@KeyFor("m1") String k1m1, @KeyFor("m2") String k1m2) {
        @KeyFor("m1") String k2m1 = identity1(k1m1);
        @KeyFor("m2") String k2m2 = identity1(k1m2);
    }

    static @PolyKeyFor String identity1(@PolyKeyFor String arg) {
        return arg;
    }
}
