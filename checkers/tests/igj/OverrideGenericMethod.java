import checkers.igj.quals.*;

public class OverrideGenericMethod<T extends Exception> {
    void test() {
        OverrideGenericMethod<@Immutable Exception> o = null;
    }

    protected void take(T e) { }

    protected class MySubclass extends OverrideGenericMethod {
        @Override
        //:: error: (override.param.invalid)
        protected void take(Exception e) { }
    }

    protected class MySubclass2 extends OverrideGenericMethod {
        @Override
        // The raw type in the super class is treated like a
        // wildcard, therefore also this doesn't match.
        //:: error: (override.param.invalid)
        protected void take(@Immutable Exception e) { }
    }

    protected class MySubclass3 extends OverrideGenericMethod<@Immutable Exception> {
        @Override
        //:: error: (override.param.invalid)
        protected void take(Exception e) { }
    }

    protected class MySubclass4 extends OverrideGenericMethod<@Immutable Exception> {
        @Override
        protected void take(@Immutable Exception e) { }
    }
}
