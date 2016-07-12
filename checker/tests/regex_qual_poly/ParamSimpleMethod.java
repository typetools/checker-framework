// Simple test for qualifier parameters on methods.
import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.qualframework.poly.qual.Wildcard;

// Test method qual parameters
@ClassRegexParam("Main")
class ParamSimpleMethodA {}

abstract class ParamSimpleMethod {
    @MethodRegexParam("Main")
    static void test(
            @Var(arg = "Main", param = "Main") ParamSimpleMethodA i,
            @Var(arg = "Main", param = "Main") ParamSimpleMethodA j) {}

    @MethodRegexParam("Main")
    @Var(arg = "main", param = "Main") ParamSimpleMethodA test2(
            @Var(arg = "Main", param = "Main") ParamSimpleMethodA in, ParamSimpleMethodA other) {
        //:: error: (return.type.incompatible)
        return makeTainted();
    }

    abstract @Regex(param = "Main") ParamSimpleMethodA makeTainted();

    abstract @Regex(value = 1, param = "Main") ParamSimpleMethodA makeUntainted();

    abstract void takeTainted(@Regex(param = "Main") ParamSimpleMethodA o);

    abstract void takeUntainted(@Regex(value = 1, param = "Main") ParamSimpleMethodA o);

    void test() {
        test(makeTainted(), makeTainted());
        //:: error: (argument.type.incompatible)
        test(makeTainted(), makeUntainted());
        test(makeUntainted(), makeUntainted());
    }
}
