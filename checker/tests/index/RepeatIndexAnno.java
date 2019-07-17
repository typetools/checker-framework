import org.checkerframework.checker.index.qual.EnsuresLTLengthOf;
import org.checkerframework.checker.index.qual.EnsuresLTLengthOfIf;

class RepeatIndexAnno {

    protected String value1;
    protected String value2;
    protected String value3;
    protected int v1;
    protected int v2;
    protected int v3;

    @EnsuresLTLengthOf(value = "v1", targetValue = "value1", offset = "1")
    @EnsuresLTLengthOf(value = "v2", targetValue = "value2", offset = "2")
    @EnsuresLTLengthOf(value = "v3", targetValue = "value3", offset = "3")
    public void func1() {
        v1 = value1.length() - 2;
        v2 = value2.length() - 3;
        v3 = value3.length() - 4;
    }

    @EnsuresLTLengthOfIf(expression = "v1", targetValue = "value1", offset = "1", result = false)
    @EnsuresLTLengthOfIf(expression = "v2", targetValue = "value2", offset = "2", result = false)
    @EnsuresLTLengthOfIf(expression = "v3", targetValue = "value3", offset = "3", result = true)
    public boolean func2() {
        v1 = value1.length() - 2;
        v2 = value2.length() - 3;
        v3 = value3.length() + 4;
        return false;
    }
}
