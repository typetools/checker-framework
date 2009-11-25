import checkers.interning.quals.*;

import java.util.*;

public class InternMethod {

    private static Map<Foo, @Interned Foo> pool =
        new HashMap<Foo, @Interned Foo>();

    class Foo {

        @SuppressWarnings("interning")
        public @Interned Foo intern() {
            if (!pool.containsKey(this))
                pool.put(this, (@Interned Foo)this);
            return pool.get(this);
        }
    }

    void test() {
        Foo f = new Foo();
        @Interned Foo g = f.intern();
    }

    public static @Interned String intern(String a) {
        return (a == null) ? null : a.intern();
  }


}
