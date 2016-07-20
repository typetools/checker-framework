// Simple test for qualifier parameters on methods.
import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.qualframework.poly.qual.Wildcard;

// Polymorphic method parameters
@ClassRegexParam("Main")
class PpA {}

abstract class ParamPoly {
    abstract void test(@PolyRegex(param = "Main") PpA i, @PolyRegex(param = "Main") PpA j);

    abstract @PolyRegex(param = "Main") PpA id(@PolyRegex(param = "Main") PpA i);

    abstract @Regex(param = "Main") PpA makeTainted();

    abstract @Regex(value = 1, param = "Main") PpA makeUntainted();

    abstract void takeTainted(@Regex(param = "Main") PpA o);

    abstract void takeUntainted(@Regex(value = 1, param = "Main") PpA o);

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
