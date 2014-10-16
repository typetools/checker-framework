// Test qualifier parameter combining.
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

@TaintingParam("Main")
class A {
    // B<<Main + TAINTED>> x;
    public @Var(value="Main", target="Main2") @Tainted(target="Main2") B x;
    // B<<Main + UNTAINTED>> y;
    public @Var(value="Main", target="Main2") @Untainted(target="Main2") B y;
    // B<<Main>> z;
    public @Var(value="Main", target="Main2") B z;
}

@TaintingParam("Main2")
class B { }

abstract class Test {
    abstract @Tainted(target="Main") A makeTainted();
    abstract @Untainted(target="Main") A makeUntainted();

    abstract void takeTainted(@Tainted(target="Main2") B o);
    abstract void takeUntainted(@Untainted(target="Main2") B o);

    void test() {
        @Tainted(target="Main") A ta = makeTainted();
        @Untainted(target="Main") A ua = makeUntainted();

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
