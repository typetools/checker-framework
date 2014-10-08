// Test parameter defaulting rules.
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

@TaintingParam("Main")
class A {
    public @Var("Main") Integer z;
}

abstract class Test {
    // Defaults to A<<?>> (? is the top of the containment hierarchy, so A<<?>>
    // is the top of the hierarchy of instantiations of A).
    abstract A make();
    abstract @Tainted A makeTainted();

    abstract void takeTainted(@Tainted Integer o);
    abstract void takeUntainted(@Untainted Integer o);
    abstract void take(Integer o);
    abstract void takeA(A a);

    void test() {
        A a = make();
        @Tainted A ta = makeTainted();

        //:: error: (argument.type.incompatible)
        takeUntainted(a.z);
        //:: error: (argument.type.incompatible)
        takeTainted(a.z);
        take(a.z);

        takeA(a);
        takeA(ta);
    }
}
