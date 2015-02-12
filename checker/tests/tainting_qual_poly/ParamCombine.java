// Test qualifier parameter combining.
import org.checkerframework.checker.tainting.qual.*;

@ClassTaintingParam("Main")
class A {
    // B<<Main + TAINTED>> x;
    public @Var(arg="Main", param="Main2") @Tainted(param="Main2") B x;
    // B<<Main + UNTAINTED>> y;
    public @Var(arg="Main", param="Main2") @Untainted(param="Main2") B y;
    // B<<Main>> z;
    public @Var(arg="Main", param="Main2") B z;
}

@ClassTaintingParam("Main2")
class B { }

abstract class Test {
    abstract @Tainted(param="Main") A makeTainted();
    abstract @Untainted(param="Main") A makeUntainted();

    abstract void takeTainted(@Tainted(param="Main2") B o);
    abstract void takeUntainted(@Untainted(param="Main2") B o);

    void test() {
        @Tainted(param="Main") A ta = makeTainted();
        @Untainted(param="Main") A ua = makeUntainted();

        takeTainted(ta.x);
        takeTainted(ta.y);
        takeTainted(ta.z);
        takeTainted(ua.x);
        //:: error: (argument.type.incompatible)
        takeTainted(ua.y);
        //:: error: (argument.type.incompatible)
        takeTainted(ua.z);

        //:: error: (argument.type.incompatible)
        takeUntainted(ta.x);
        // The combining rule for Tainting is LUB, so the type of ta.y is
        // B<<TAINTED + UNTAINTED>> = B<<TAINTED>>.
        //:: error: (argument.type.incompatible)
        takeUntainted(ta.y);
        //:: error: (argument.type.incompatible)
        takeUntainted(ta.z);

        //:: error: (argument.type.incompatible)
        takeUntainted(ua.x);
        takeUntainted(ua.y);
        takeUntainted(ua.z);
    }
}
