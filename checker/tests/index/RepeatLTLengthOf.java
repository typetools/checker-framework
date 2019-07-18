import org.checkerframework.checker.index.qual.EnsuresLTLengthOf;
import org.checkerframework.checker.index.qual.EnsuresLTLengthOfIf;

class RepeatLTLengthOf {

    protected String value1;
    protected String value2;
    protected String value3;
    protected int v1;
    protected int v2;
    protected int v3;

    public void func1() {
        v1 = value1.length() - 1;
        v2 = value2.length() - 3;
        v3 = value3.length() - 4;
    }

    public boolean func2() {
        v1 = value1.length() - 3;
        v2 = value2.length() - 2;
        v3 = value3.length() - 5;
        return true;
    }

    @EnsuresLTLengthOf(value = "v1", targetValue = "value1", offset = "1")
    @EnsuresLTLengthOf(value = "v2", targetValue = "value2", offset = "2")
    @EnsuresLTLengthOf(value = "v3", targetValue = "value3", offset = "3")
    // :: error:  (contracts.postcondition.not.satisfied)
    public void samefunc1() {
        v1 = value1.length() - 1; // condition not satisfied here
        v2 = value2.length() - 3;
        v3 = value3.length() - 4;
    }

    @EnsuresLTLengthOfIf(expression = "v1", targetValue = "value1", offset = "2", result = true)
    @EnsuresLTLengthOfIf(expression = "v2", targetValue = "value2", offset = "3", result = true)
    @EnsuresLTLengthOfIf(expression = "v3", targetValue = "value3", offset = "4", result = true)
    public boolean samefunc2() {
        v1 = value1.length() - 3;
        v2 = value2.length() - 2; // condition not satisfied here
        v3 = value3.length() - 5;
        // :: error:  (contracts.conditional.postcondition.not.satisfied)
        return true;
    }

    @EnsuresLTLengthOf.List({
        @EnsuresLTLengthOf(value = "v1", targetValue = "value1", offset = "1"),
        @EnsuresLTLengthOf(value = "v2", targetValue = "value2", offset = "2")
    })
    @EnsuresLTLengthOf(value = "v3", targetValue = "value3", offset = "3")
    public void func3() {
        v1 = value1.length() - 2;
        v2 = value2.length() - 3;
        v3 = value3.length() - 4;
    }

    @EnsuresLTLengthOfIf.List({
        @EnsuresLTLengthOfIf(
                expression = "v1",
                targetValue = "value1",
                offset = "2",
                result = true),
        @EnsuresLTLengthOfIf(expression = "v2", targetValue = "value2", offset = "3", result = true)
    })
    @EnsuresLTLengthOfIf(expression = "v3", targetValue = "value3", offset = "4", result = true)
    public boolean func4() {
        v1 = value1.length() - 3;
        v2 = value2.length() - 4;
        v3 = value3.length() - 5;
        return true;
    }
}
