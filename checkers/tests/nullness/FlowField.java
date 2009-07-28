import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.Nullable")
public class FlowField {

    public @Nullable String s = null;

    void test() {
        if (s != null) {
          s.startsWith ("foo");
        }
    }

    static String field = "asdf";

    static {
        field = "asdf";
    }

    void testFields() {
        System.out.println(field.length());
    }

    // Fowrard reference to static finals
    void test1() {
        nonnull.toString();
    }

    static final String nonnull = new String();

    class A {
        protected String field = null;
    }

    class B extends A {
        void test() {
            assert field != null;
            field.length();
        }
    }

    static class BooleanWrapper {
        @Nullable Object b;
    }
    @Nullable BooleanWrapper bw;

    void testBitwise(@NonNull FlowField a, @NonNull FlowField b) {
        Object r;
        if (a.bw != null)
            if (b.bw != null) {
                r = a.bw.b;
                r = b.bw.b;
            }
        if (a.bw != null && b.bw != null) {
            r = a.bw.b;
            r = b.bw.b;
        }
    }

    void testInstanceOf(@NonNull FlowField a) {
        if (!(a.s instanceof String))
            return;
        @NonNull String s = a.s;
    }

    void testTwoLevels(@NonNull FlowField a, BooleanWrapper bwArg) {
        if (!(a.bw.hashCode() == 0)) // warning here
            return;
        Object o = a.bw.b;      // but not here
    }
}
