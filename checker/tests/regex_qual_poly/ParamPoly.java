// Simple test for qualifier parameters on methods.
import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.qualframework.poly.qual.Wildcard;

// Polymorphic method parameters
@ClassRegexParam("Main")
class A { }

abstract class Test {
    abstract void test(@PolyRegex(param="Main") A i, @PolyRegex(param="Main") A j);
    abstract @PolyRegex(param="Main") A id(@PolyRegex(param="Main") A i);

    abstract @Regex(param="Main") A makeTainted();
    abstract @Regex(value=1, param="Main") A makeUntainted();

    abstract void takeTainted(@Regex(param="Main") A o);
    abstract void takeUntainted(@Regex(value=1, param="Main") A o);

    void test() {
        test(makeTainted(), makeTainted());
        //:: error: (argument.type.incompatible)
        test(makeTainted(), makeUntainted());
        test(makeUntainted(), makeUntainted());

        takeTainted(id(makeTainted()));
        //:: error: (argument.type.incompatible)
        takeTainted(id(makeUntainted()));
        //:: error: (argument.type.incompatible)
        takeUntainted(id(makeTainted()));
        takeUntainted(id(makeUntainted()));
    }
}
