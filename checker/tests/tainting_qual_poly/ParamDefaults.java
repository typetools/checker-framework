// Test parameter defaulting rules.
import org.checkerframework.checker.tainting.qual.*;

@ClassTaintingParam("Main")
class A {
    public @Var(arg="Main", param="Main2") B z;
}

@ClassTaintingParam("Main2")
class B { }

abstract class Test {
    // Defaults to A<<?>> (? is the top of the containment hierarchy, so A<<?>>
    // is the top of the hierarchy of instantiations of A).
    abstract A make();
    abstract @Tainted(param="Main") A makeTainted();

    abstract void takeTainted(@Tainted(param="Main2") B o);
    abstract void takeUntainted(@Untainted(param="Main2") B o);
    abstract void take(B o);
    abstract void takeA(A a);

    void test() {
        A a = make();
        @Tainted(param="Main") A ta = makeTainted();

        //:: error: (argument.type.incompatible)
        takeUntainted(a.z);
        //:: error: (argument.type.incompatible)
        takeTainted(a.z);
        take(a.z);

        takeA(a);
        takeA(ta);
    }
}
