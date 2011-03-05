import checkers.nullness.quals.*;

/*
 * This test is based on Issue 93:
 * http://code.google.com/p/checker-framework/issues/detail?id=93
 */
public class MethodTypeVars {
    void m() {
        //:: (argument.type.incompatible)
        Object a = A.badMethod(null);
        Object b = A.badMethod(new Object());
                
        //:: (argument.type.incompatible)
        A.goodMethod(null);
        A.goodMethod(new Object());
    }
}

class A{
    public static <T extends @NonNull Object> T badMethod(T t) {
        return (T) new Object();
    }
    
    public static <T extends @NonNull Object> void goodMethod(T t) {}
}
