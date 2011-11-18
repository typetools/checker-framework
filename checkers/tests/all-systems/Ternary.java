import checkers.nullness.quals.Nullable;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.*;

class Ternary {
    void m1(boolean b) {
        String s = b ? new String("foo") : null;
    }

    void m2(boolean b) {
        String s = b ? null : new String("foo");
    }

    @Nullable String m3(boolean b) {
        return b ? new String("foo") : null;
    }

    void m4(boolean b) {
        String[] s = b ? new String[5] : null;
    }

    void m5(boolean b) {
        Object o = new Object();
        String s = b ? (String) o : null;
    }

    void m6(boolean b) {
        String p = "x*(((";
        String s = b ? p : null;
    }

    class Generic<T extends @Nullable Object> {
        void cond(boolean b, T p) {
            T r1 = b ? p : null;
            T r2 = b ? null : p;
        }

        void cond2(boolean b, T p) {
            T r1 = b ? null : p;
        }

        void cond(boolean b, T p1, T p2) {
            p1 = b ? p1 : p2;
        }
    }

    void array(boolean b) {
        String[] s = b ? new String[5] : null;
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
        Object s = (m!=null) ? m : c;
    }

    void vars2() {
        // ClassSymbol and MethodSymbol generate an intersection type.
        ClassSymbol c = null;
        MethodSymbol m = null;
        Symbol s = (m!=null) ? m : c;
    }
}