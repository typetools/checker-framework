import checkers.igj.quals.*;

public class OverrideGenericMethod<T extends Exception> {
    void test() {
        OverrideGenericMethod<@Immutable Exception> o = null;
    }

    protected void take(T e) { }

    protected class MySubclass extends OverrideGenericMethod {
        @Override
        protected void take(Exception e) { }
    }
}
