import org.checkerframework.common.value.qual.EnsuresMinLenIf;

public class RepeatMinLenIf {

    protected String a;
    protected String b;
    protected String c;

    public boolean func1() {
        a = "checker";
        c = "framework";
        b = "hello";
        return true;
    }

    @EnsuresMinLenIf(
            expression = {"a", "b"},
            targetValue = 6,
            result = true)
    @EnsuresMinLenIf(expression = "c", targetValue = 6, result = true)
    public boolean withcondpostconditionsfunc1() {
        a = "checker";
        c = "framework";
        b = "hello"; // condition not satisfied here
        // :: error:  (contracts.conditional.postcondition.not.satisfied)
        return true;
    }

    @EnsuresMinLenIf.List({
        @EnsuresMinLenIf(expression = "a", targetValue = 6, result = true),
        @EnsuresMinLenIf(expression = "b", targetValue = 4, result = true)
    })
    @EnsuresMinLenIf(expression = "c", targetValue = 6, result = true)
    public boolean withcondpostconditionfunc1() {
        a = "checker";
        c = "framework";
        b = "hello";
        return true;
    }
}
