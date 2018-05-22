import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.*;
import java.lang.ref.WeakReference;

class Ternary<F> {
    void m1(boolean b) {
        String s = b ? new String("foo") : null;
    }

    void m2(boolean b) {
        String s = b ? null : new String("foo");
    }

    @SuppressWarnings("nullness") // Don't want to depend on @Nullable
    String m3(boolean b) {
        return b ? new String("foo") : null;
    }

    void m4(boolean b) {
        String[] s = b ? new String[] {""} : null;
    }

    void m5(boolean b) {
        Object o = new Object();
        String s = b ? (String) o : null;
    }

    void m6(boolean b) {
        String p = "x*(((";
        String s = b ? p : null;
    }

    class Generic<T extends Object> {
        void cond(boolean b, T p1, T p2) {
            p1 = b ? p1 : p2;
        }
    }

    void array(boolean b) {
        String[] s = b ? new String[] {""} : null;
    }

    void generic(boolean b, Generic<String> p) {
        Generic<String> s = b ? p : null;
    }

    void primarray(boolean b) {
        long[] result = b ? null : new long[10];
    }

    void vars() {
        // ClassSymbol and MethodSymbol generate an intersection type.
        ClassSymbol c = null;
        MethodSymbol m = null;
        Object s = (m != null) ? m : c;
    }

    void vars2() {
        // ClassSymbol and MethodSymbol generate an intersection type.
        ClassSymbol c = null;
        MethodSymbol m = null;
        Symbol s = (m != null) ? m : c;
    }

    public void test(MyWeakRef<? extends F> existingRef) {
        @SuppressWarnings("known.nonnull")
        F existing = existingRef == null ? null : existingRef.get();
    }

    private static final class MyWeakRef<L> extends WeakReference<L> {

        public MyWeakRef(L referent) {
            super(referent);
        }
    }
}
