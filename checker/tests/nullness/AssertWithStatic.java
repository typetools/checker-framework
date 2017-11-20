import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

public class AssertWithStatic {

    static @Nullable String f;

    @EnsuresNonNullIf(result = true, expression = "AssertWithStatic.f")
    public boolean hasSysOut1() {
        return AssertWithStatic.f != null;
    }

    @EnsuresNonNullIf(result = true, expression = "f")
    public boolean hasSysOut2() {
        return AssertWithStatic.f != null;
    }

    @EnsuresNonNullIf(result = true, expression = "AssertWithStatic.f")
    public boolean hasSysOut3() {
        return f != null;
    }

    @EnsuresNonNullIf(result = true, expression = "f")
    public boolean hasSysOut4() {
        return f != null;
    }

    @EnsuresNonNullIf(result = false, expression = "AssertWithStatic.f")
    public boolean noSysOut1() {
        return AssertWithStatic.f == null;
    }

    @EnsuresNonNullIf(result = false, expression = "f")
    public boolean noSysOut2() {
        return AssertWithStatic.f == null;
    }

    @EnsuresNonNullIf(result = false, expression = "AssertWithStatic.f")
    public boolean noSysOut3() {
        return f == null;
    }

    @EnsuresNonNullIf(result = false, expression = "f")
    public boolean noSysOut4() {
        return f == null;
    }

    @EnsuresNonNull("AssertWithStatic.f")
    // :: error: (contracts.postcondition.not.satisfied)
    public void sysOutAfter1() {}

    @EnsuresNonNull("f")
    // :: error: (contracts.postcondition.not.satisfied)
    public void sysOutAfter2() {}
}
