import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RepeatNonNull {

    protected @Nullable String value1;
    protected @Nullable String value2;
    protected @Nullable String value3;

    public boolean func1() {
        value1 = "value1";
        value2 = "value2";
        value3 = null;
        return true;
    }

    public void func2() {
        value1 = "value1";
        value2 = null;
        value3 = "value3";
    }

    // Error occurred because "value3" should not be null if the return is true as described in the
    // postcondition.
    @EnsuresNonNullIf(
            expression = {"value1", "value2"},
            result = true)
    @EnsuresNonNullIf(expression = "value3", result = true)
    public boolean withcondpostconditionsfunc1() {
        value1 = "value1";
        value2 = "value2";
        value3 = null; // condition not satisfied here
        // :: error:  (contracts.conditional.postcondition.not.satisfied)
        return true;
    }

    // Error occurred because "value2" should not be null as described in the postcondition.
    @EnsuresNonNull("value1")
    @EnsuresNonNull(value = {"value2", "value3"})
    // :: error:  (contracts.postcondition.not.satisfied)
    public void withpostconditionsfunc2() {
        value1 = "value1";
        value2 = null; // condition not satisfied here
        value3 = "value3";
    }

    @EnsuresNonNullIf.List({
        @EnsuresNonNullIf(expression = "value1", result = true),
        @EnsuresNonNullIf(expression = "value2", result = true),
    })
    @EnsuresNonNullIf(expression = "value3", result = false)
    public boolean withcondpostconditionfunc1() {
        value1 = "value1";
        value2 = "value2";
        value3 = null;
        return true;
    }

    @EnsuresNonNull.List({@EnsuresNonNull("value1")})
    @EnsuresNonNull("value3")
    public void withpostconditionfunc2() {
        value1 = "value1";
        value2 = null;
        value3 = "value3";
    }
}
