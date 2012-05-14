import checkers.nullness.quals.*;

/*
 * This test is based on Issue 93:
 * http://code.google.com/p/checker-framework/issues/detail?id=93
 */
public class MethodTypeVars {
    void m() {
        //:: error: (type.argument.type.incompatible)
        Object a = A.badMethod(null);
        Object b = A.badMethod(new Object());
                
        //:: error: (argument.type.incompatible)
        A.goodMethod(null);
        A.goodMethod(new Object());
    }
}

class A {
    public static <T extends @NonNull Object> T badMethod(T t) {
        return (T) new Object();
    }
    
    public static <T extends @NonNull Object> void goodMethod(T t) {}
}

class B {
    public <T> void indexOf1(T[] a, /*@Nullable*/ Object elt) {}
    // This is not valid Java syntax.
    // public void indexOf2(?[] a, /*@Nullable*/ Object elt) {}
    
    void call() {
        Integer[] arg = new Integer[4];
        indexOf1(arg, new Integer(5));
        // indexOf2(arg, new Integer(5));
    }
}