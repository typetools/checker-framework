import org.checkerframework.checker.index.qual.EnsuresLTLengthOf;
import org.checkerframework.checker.index.qual.EnsuresLTLengthOfIf;

public class RepeatLTLengthOf {

    protected String value1;
    protected String value2;
    protected String value3;
    protected int v1;
    protected int v2;
    protected int v3;

    public void func1() {
        v1 = value1.length() - 3;
        v2 = value2.length() - 3;
        v3 = value3.length() - 3;
    }

    public boolean func2() {
        v1 = value1.length() - 3;
        v2 = value2.length() - 3;
        v3 = value3.length() - 3;
        return true;
    }

    @EnsuresLTLengthOf(value = "v1", targetValue = "value1", offset = "2")
    @EnsuresLTLengthOf(value = "v2", targetValue = "value2", offset = "1")
    @EnsuresLTLengthOf(value = "v3", targetValue = "value3", offset = "0")
    public void client1() {
        withpostconditionsfunc1();
    }

    @EnsuresLTLengthOfIf(expression = "v1", targetValue = "value1", offset = "2", result = true)
    @EnsuresLTLengthOfIf(expression = "v2", targetValue = "value2", offset = "1", result = true)
    @EnsuresLTLengthOfIf(expression = "v3", targetValue = "value3", offset = "0", result = true)
    public boolean client2() {
        return withcondpostconditionsfunc2();
    }

    @EnsuresLTLengthOf.List({
        @EnsuresLTLengthOf(value = "v1", targetValue = "value1", offset = "2"),
        @EnsuresLTLengthOf(value = "v2", targetValue = "value2", offset = "1")
    })
    @EnsuresLTLengthOf(value = "v3", targetValue = "value3", offset = "0")
    public void client3() {
        withpostconditionfunc1();
    }

    @EnsuresLTLengthOfIf.List({
        @EnsuresLTLengthOfIf(
                expression = "v1",
                targetValue = "value1",
                offset = "2",
                result = true),
        @EnsuresLTLengthOfIf(expression = "v2", targetValue = "value2", offset = "1", result = true)
    })
    @EnsuresLTLengthOfIf(expression = "v3", targetValue = "value3", offset = "0", result = true)
    public boolean client4() {
        return withcondpostconditionfunc2();
    }

    @EnsuresLTLengthOf(value = "v1", targetValue = "value1", offset = "2")
    @EnsuresLTLengthOf(value = "v2", targetValue = "value2", offset = "1")
    @EnsuresLTLengthOf(value = "v3", targetValue = "value3", offset = "0")
    public void withpostconditionsfunc1() {
        v1 = value1.length() - 3;
        v2 = value2.length() - 3;
        v3 = value3.length() - 3;
    }

    @EnsuresLTLengthOfIf(expression = "v1", targetValue = "value1", offset = "2", result = true)
    @EnsuresLTLengthOfIf(expression = "v2", targetValue = "value2", offset = "1", result = true)
    @EnsuresLTLengthOfIf(expression = "v3", targetValue = "value3", offset = "0", result = true)
    public boolean withcondpostconditionsfunc2() {
        v1 = value1.length() - 3;
        v2 = value2.length() - 3;
        v3 = value3.length() - 3;
        return true;
    }

    @EnsuresLTLengthOf.List({
        @EnsuresLTLengthOf(value = "v1", targetValue = "value1", offset = "2"),
        @EnsuresLTLengthOf(value = "v2", targetValue = "value2", offset = "1")
    })
    @EnsuresLTLengthOf(value = "v3", targetValue = "value3", offset = "0")
    public void withpostconditionfunc1() {
        v1 = value1.length() - 3;
        v2 = value2.length() - 3;
        v3 = value3.length() - 3;
    }

    @EnsuresLTLengthOfIf.List({
        @EnsuresLTLengthOfIf(
                expression = "v1",
                targetValue = "value1",
                offset = "2",
                result = true),
        @EnsuresLTLengthOfIf(expression = "v2", targetValue = "value2", offset = "1", result = true)
    })
    @EnsuresLTLengthOfIf(expression = "v3", targetValue = "value3", offset = "0", result = true)
    public boolean withcondpostconditionfunc2() {
        v1 = value1.length() - 3;
        v2 = value2.length() - 3;
        v3 = value3.length() - 3;
        return true;
    }
}
