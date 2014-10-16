// Simple test for qualifier parameters on classes.
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

// Test qual param on a class
@ClassTaintingParam("Main")
class A {
    public @Tainted(target="Main2") B x;
    public @Untainted(target="Main2") B y;
    public @Var(value="Main", target="Main2") B z;
}

@ClassTaintingParam("Main2")
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
        //:: error: (argument.type.incompatible)
        takeTainted(ta.y);
        takeTainted(ta.z);

        takeTainted(ua.x);
        //:: error: (argument.type.incompatible)
        takeTainted(ua.y);
        //:: error: (argument.type.incompatible)
        takeTainted(ua.z);


        //:: error: (argument.type.incompatible)
        takeUntainted(ta.x);
        takeUntainted(ta.y);
        //:: error: (argument.type.incompatible)
        takeUntainted(ta.z);

        //:: error: (argument.type.incompatible)
        takeUntainted(ua.x);
        takeUntainted(ua.y);
        takeUntainted(ua.z);
    }
}
