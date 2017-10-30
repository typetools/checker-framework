import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.PolyAll;

// test related to issue 429: https://github.com/typetools/checker-framework/issues/429
class KeyForPolymorphism {

    Map<String, Object> m1 = new HashMap<String, Object>();
    Map<String, Object> m2 = new HashMap<String, Object>();

    void method(@KeyFor("m1") String k1m1, @KeyFor("m2") String k1m2) {
        @KeyFor("m1") String k2m1 = identity1(k1m1);
        @KeyFor("m2") String k2m2 = identity1(k1m2);
        @KeyFor("m1") String k3m1 = identity2(k1m1);
        @KeyFor("m2") String k3m2 = identity2(k1m2);
    }

    static @PolyKeyFor String identity1(@PolyKeyFor String arg) {
        return arg;
    }

    static @PolyAll String identity2(@PolyAll String arg) {
        return arg;
    }
}
