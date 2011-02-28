import java.util.*;
import checkers.javari.quals.*;

public class RandomTests {

    public byte[] GOOD_HDR = {};

    void testArray() {
        byte[] array = GOOD_HDR;
    }

    void testBoxing() {
        Integer i = 0;
        int m = i;
        Integer n = m;
    }

    void testMapBoxing() {
        Map<String, Integer> m = null;
        int i = m.get("m");
    }

    // Test static block assignment
    static @ReadOnly String s;
    static {
        s = "m";
    }

    // Test constructors with var args
    public RandomTests(String s, String... args) { }

    public void testConstructor() {
        new RandomTests("m", "n", "d");
    }

    // Test for wildcards supertypes
    public void testWildcards() {
        List<?> l = null;
        @ReadOnly Object o = l.get(0);
    }

    // Deal with enum set
    public enum MyEnum { P; }

    public void testEnum() {
        EnumSet<MyEnum> m;
    }

    // Same type args
    static class Pair<T1, T2> { }

    static class MyClass<C> {
        Pair<C, C> t;
    }

    // Casts
    public void cast() {
        List<String> l = (List<String>) new HashMap<String, String>();
    }

    public <T> void test(Map<T, String> m, T key) {
        // javari bug
        //m.get(key);
    }

    void testIntersection() {
        java.util.Arrays.asList("m", 1);
    }

}
