import org.checkerframework.checker.interning.qual.*;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Polymorphism {
    // Test parameter
    public @PolyInterned String identity(@PolyInterned String s) {
        return s;
    }

    void testParam() {
        String notInterned = new String("not interned");
        @Interned String interned = "interned";

        interned = identity(interned);
        // :: error: (assignment.type.incompatible)
        interned = identity(notInterned); // invalid
    }

    // test as receiver
    @PolyInterned Polymorphism getSelf(@PolyInterned Polymorphism this) {
        return this;
    }

    void testReceiver() {
        Polymorphism notInterned = new Polymorphism();
        @Interned Polymorphism interned = null;

        interned = interned.getSelf();
        // :: error: (assignment.type.incompatible)
        interned = notInterned.getSelf(); // invalid
    }

    // Test assinging interned to PolyInterned
    public @PolyInterned String always(@PolyInterned String s) {
        if (s.equals("n")) {
            // This code type-checkd when the hierarchy contained just @UnknownInterned and
            // @Interned, but no longer does because of @InternedDistinct.
            // :: error: (return.type.incompatible)
            return "m";
        } else {
            // :: error: (return.type.incompatible)
            return new String("m"); // invalid
        }
    }

    public static @PolyInterned Object[] id(@PolyInterned Object[] a) {
        return a;
    }

    public static void idTest(@Interned Object @Interned [] seq) {
        @Interned Object[] copy_uninterned = id(seq);
    }

    private static Map<
                    List<@Interned String @Interned []>,
                    WeakReference<@Interned String @Interned []>>
            internedStringSequenceAndIndices;
    private static List<@Interned String @Interned []> sai;
    private static WeakReference<@Interned String @Interned []> wr;

    public static void testArrayInGeneric() {
        internedStringSequenceAndIndices.put(sai, wr);
    }

    // check for a crash when using raw types
    void processMap(Map<String, String> map) {}

    void testRaw() {
        Map m = null;
        // TODO: RAW TYPES WILL EVENTUALLY REQUIRE THAT THERE BOUNDS BE EXACTLY THE QUALIFIER
        // EXPECTED.
        // :: warning: [unchecked] unchecked method invocation: method processMap in class
        // Polymorphism is applied to given types :: warning: [unchecked] unchecked conversion
        processMap(m);
    }

    // test anonymous classes
    private void testAnonymous() {
        new Object() {
            @org.checkerframework.dataflow.qual.Pure
            public boolean equals(Object o) {
                return true;
            }
        }.equals(null);

        Date d = new Date() {};
    }
}
