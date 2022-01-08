package keyfor;

import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.checker.nullness.qual.PolyKeyFor;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;

import java.util.HashMap;
import java.util.Map;

public class KeyForLub {
    public static boolean flag;
    Map<Object, Object> map1 = new HashMap<>();
    Map<Object, Object> map2 = new HashMap<>();
    Map<Object, Object> map3 = new HashMap<>();

    void method(
            @KeyFor({"map1", "map2"}) String key12,
            @KeyFor({"map1", "map3"}) String key13,
            @UnknownKeyFor String unknown) {
        @KeyFor("map1") String key1 = flag ? key12 : key13;

        // :: error: (assignment.type.incompatible)
        @KeyFor({"map1", "map2"}) String key2 = flag ? key12 : key13;

        // :: error: (assignment.type.incompatible)
        @KeyFor({"map1", "map2"}) String key3 = flag ? key12 : unknown;
    }

    @PolyKeyFor String poly1(@KeyFor("map1") String key1, @PolyKeyFor String poly) {
        // :: error: (conditional.type.incompatible)
        return flag ? key1 : poly;
    }

    void poly2(@PolyKeyFor String poly, @UnknownKeyFor String unknown, @KeyForBottom String bot) {
        // :: error: (assignment.type.incompatible)
        @PolyKeyFor String s1 = flag ? poly : unknown;
        @PolyKeyFor String s2 = flag ? poly : bot;
        // :: error: (assignment.type.incompatible)
        @KeyForBottom String s3 = flag ? poly : bot;
    }
}
