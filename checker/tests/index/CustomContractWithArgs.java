import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.common.value.qual.MinLen;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.PostconditionAnnotation;

// Postcondition for MinLen
@PostconditionAnnotation(
    qualifier = MinLen.class,
    sourceArguments = "targetValue",
    targetArguments = "value"
)
@interface EnsuresMinLen {
    public String[] value();

    public int targetValue();
}

// Conditional postcondition for LTLengthOf
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
    void minLenContract(int[] a) {
        if (a.length < 10) throw new RuntimeException();
    }

    @EnsuresMinLen(value = "#1", targetValue = 10)
    // :: error: (contracts.postcondition.not.satisfied)
    void minLenWrong(int[] a) {
        if (a.length < 9) throw new RuntimeException();
    }

    void minLenUse(int[] b) {
        minLenContract(b);
        int @MinLen(10) [] c = b;
    }

    @EnsuresLTLIf(expression = "#2", targetValue = "#1", targetOffset = "#3", result = true)
    boolean ltlContract(int[] a, int b, int c) {
        return b + c < a.length;
    }

    void ltlUse(int[] a, int b, int c) {
        if (ltlContract(a, b, c)) {
            @LTLengthOf(value = "a", offset = "c") int i = b;
        }
        // :: error: (assignment.type.incompatible)
        @LTLengthOf(value = "a", offset = "c") int j = b;
    }
}
