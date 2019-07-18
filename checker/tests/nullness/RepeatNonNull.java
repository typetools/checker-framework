import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RepeatNonNull {

    protected @Nullable String value1;
    protected @Nullable String value2;
    protected @Nullable String value3;
    protected @Nullable String value4;

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

    @EnsuresNonNullIf(
            expression = {"value1", "value2"},
            result = true)
    @EnsuresNonNullIf(expression = "value3", result = true)
    public boolean samefunc1() {
        value1 = "value1";
        value2 = "value2";
        value3 = null; // condition not satisfied here
        // :: error:  (contracts.conditional.postcondition.not.satisfied)
        return true;
    }

    @EnsuresNonNull("value1")
    @EnsuresNonNull(value = {"value2", "value3"})
    // :: error:  (contracts.postcondition.not.satisfied)
    public void samefunc2() {
        value1 = "value1";
        value2 = null; // condition not satisfied here
        value3 = "value3";
    }

    @EnsuresNonNullIf.List({
        @EnsuresNonNullIf(
                expression = {"value1", "value2"},
                result = true),
        @EnsuresNonNullIf(expression = "value3", result = true)
    })
    @EnsuresNonNullIf(expression = "value4", result = true)
    public boolean func3() {
        value1 = "value1";
        value2 = "value2";
        value3 = "value3";
        value4 = "value4";
        return true;
    }

    @EnsuresNonNull.List({@EnsuresNonNull("value1"), @EnsuresNonNull(value = {"value2", "value3"})})
    @EnsuresNonNull("value4")
    public void func4() {
        value1 = "value1";
        value2 = "value2";
        value3 = "value3";
        value4 = "value4";
    }
}
