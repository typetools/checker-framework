import org.checkerframework.common.value.qual.EnsuresMinLenIf;

public class RepeatValueAnno {

    protected String a;
    protected String b;
    protected String c;

    @EnsuresMinLenIf(
            expression = {"a", "b"},
            targetValue = 6,
            result = true)
    @EnsuresMinLenIf(expression = "c", targetValue = 6, result = true)
    public boolean func1() {
        a = "values";
        c = "values66";
        b = "hello"; // condition not satisfied here
        // :: error:  (contracts.conditional.postcondition.not.satisfied)
        return true;
    }
}
