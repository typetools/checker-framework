import checkers.interning.quals.*;

import java.util.*;

public class StaticInternMethod {

    private static Map<Integer, @Interned Foo> pool =
        new HashMap<Integer, @Interned Foo>();

    @SuppressWarnings("interning")
    public static @Interned Foo intern(Integer i) {
        if (pool.containsKey(i))
            return pool.get(i);

        @Interned Foo f = new @Interned Foo(i);
        pool.put(i, f);
        return f;
    }

    static class Foo {
        public Foo(Integer i) { }
    }

    void test() {
        Integer i = 0;
        Foo f = new Foo(i);
        @Interned Foo g = intern(i);
    }
}
