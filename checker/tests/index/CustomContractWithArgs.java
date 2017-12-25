import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.common.value.qual.MinLen;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.PostconditionAnnotation;

@PostconditionAnnotation(
    qualifier = MinLen.class,
    sourceArguments = "targetValue",
    targetArguments = "value"
)
@interface EnsuresMinLen {
    public String[] value();

    public int targetValue();
}

@ConditionalPostconditionAnnotation(
    qualifier = LTLengthOf.class,
    sourceArguments = {"targetValue", "targetOffset"},
    targetArguments = {"value", "offset"}
)
@interface EnsuresLTLIf {
    public boolean result();

    public String[] expression();

    public String[] targetValue();

    public String[] targetOffset();
}

public class CustomContractWithArgs {
    @EnsuresMinLen(value = "#1", targetValue = 10)
    void m1(int[] a) {}

    void n1(int[] b) {
        m1(b);
        int @MinLen(10) [] c = b;
    }

    @EnsuresLTLIf(expression = "#2", targetValue = "#1", targetOffset = "#3", result = true)
    boolean m2(int[] a, int b, int c) {
        return false;
    }

    void n2(int[] a, int b, int c) {
        if (m2(a, b, c)) {
            @LTLengthOf(value = "a", offset = "c") int i = b;
        }
        // :: error: (assignment.type.incompatible)
        @LTLengthOf(value = "a", offset = "c") int j = b;
    }
}
