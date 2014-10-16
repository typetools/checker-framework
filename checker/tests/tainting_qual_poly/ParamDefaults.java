// Test parameter defaulting rules.
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

@TaintingParam("Main")
class A {
    public @Var(value="Main", target="Main2") B z;
}

@TaintingParam("Main2")
class B { }

abstract class Test {
    // Defaults to A<<?>> (? is the top of the containment hierarchy, so A<<?>>
    // is the top of the hierarchy of instantiations of A).
    abstract A make();
    abstract @Tainted(target="Main") A makeTainted();

    abstract void takeTainted(@Tainted(target="Main2") B o);
    abstract void takeUntainted(@Untainted(target="Main2") B o);
    abstract void take(B o);
    abstract void takeA(A a);

    void test() {
        A a = make();
        @Tainted(target="Main") A ta = makeTainted();

        //:: error: (argument.type.incompatible)
        takeUntainted(a.z);
        //:: error: (argument.type.incompatible)
        takeTainted(a.z);
        take(a.z);

        takeA(a);
        takeA(ta);
    }
}
