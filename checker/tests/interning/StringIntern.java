import org.checkerframework.checker.interning.qual.*;

import java.util.*;

public class StringIntern {

    // It would be very handy (and would eliminate quite a few annotations)
    // if any final variable that is initialized to something interned
    // (essentially, to a literal) were treated as implicitly @Interned.
    final String finalStringInitializedToInterned = "foo"; // implicitly @Interned
    final String finalString2 = new String("foo");
    static final String finalStringStatic1 = "foo"; // implicitly @Interned
    static final String finalStringStatic2 = new String("foo");

    static class HasFields {
        static final String finalStringStatic3 = "foo"; // implicitly @Interned
        static final String finalStringStatic4 = new String("foo");
    }

    static class Foo {
        private static Map<Foo, @Interned Foo> pool =
            new HashMap<Foo, @Interned Foo>();

        @SuppressWarnings("interning")
        public @Interned Foo intern() {
            if (!pool.containsKey(this)) {
                pool.put(this, (@Interned Foo)this);
            }
            return pool.get(this);
        }
    }

    // Another example of the "final initialized to interned" rule
    final Foo finalFooInitializedToInterned = new Foo().intern();

    public void test(@Interned String arg) {
        String notInternedStr = new String("foo");
        @Interned String internedStr = notInternedStr.intern();
        internedStr = finalStringInitializedToInterned; // OK
        //:: error: (assignment.type.incompatible)
        internedStr = finalString2; // error
        @Interned Foo internedFoo = finalFooInitializedToInterned; // OK
        if (arg == finalStringStatic1) { }                         // OK
        //:: error: (not.interned)
        if (arg == finalStringStatic2) { }                         // error
        if (arg == HasFields.finalStringStatic3) { }               // OK
        //:: error: (not.interned)
        if (arg == HasFields.finalStringStatic4) { }               // error
    }

    private @Interned String base;
    static final String BASE_HASHCODE = "hashcode";

    public void foo() {
        if (base == BASE_HASHCODE) { }
    }

    public @Interned String emptyString(boolean b) {
        if (b) {
            return "";
        } else {
            return ("");
        }
    }

}
