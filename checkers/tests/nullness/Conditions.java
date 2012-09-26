import checkers.nullness.quals.*;

public class Conditions {
    
    @Nullable Object f;
    
    void test1(Conditions c) {
        if (!(c.f!=null))
            return;
        c.f.hashCode();
    }
    
    void test2(Conditions c) {
        if (!(c.f!=null) || 5 > 9)
            return;
        c.f.hashCode();
    }
    
    /*@AssertNonNullIfTrue("f")*/
    public boolean isNN() {
        return (f != null);
    }
    
    void test1m(Conditions c) {
        if (!(c.isNN()))
            return;
        c.f.hashCode();
    }
    void test2m(Conditions c) {
        if (!(c.isNN()) || 5 > 9)
            return;
        c.f.hashCode();
    }
}