import org.checkerframework.checker.nullness.qual.Nullable;

class InnerCrash {
    class Inner {}

    static @Nullable InnerCrash getInnerCrash() {
        return null;
    }

    static void foo() {
        // :: error: (dereference.of.nullable)
        Object o = getInnerCrash().new Inner();
    }
}
