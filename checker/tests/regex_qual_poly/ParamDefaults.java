// Test parameter defaulting rules.
import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.qualframework.poly.qual.Wildcard;

@ClassRegexParam("Main")
class A {
    public @Var(arg="Main", param="Main2") B z;
}

@ClassRegexParam("Main2")
class B { }

abstract class Test {
    // Defaults to A<<?>> (? is the top of the containment hierarchy, so A<<?>>
    // is the top of the hierarchy of instantiations of A).
    abstract A make();
    abstract @Regex(param="Main") A makeTainted();

    abstract void takeTainted(@Regex(param="Main2") B o);
    abstract void takeUntainted(@Regex(value=1, param="Main2") B o);
    abstract void take(B o);
    abstract void takeA(A a);

    void test() {
        A a = make();
        @Regex(param="Main") A ta = makeTainted();

        //:: error: (argument.type.incompatible)
        takeUntainted(a.z);
        //:: error: (argument.type.incompatible)
        takeTainted(a.z);
        take(a.z);

        takeA(a);
        takeA(ta);
    }
}
