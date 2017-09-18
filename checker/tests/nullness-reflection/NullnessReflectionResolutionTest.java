import java.lang.reflect.Method;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.reflection.qual.MethodVal;

/**
 * Testing that reflection resolution uses more precise annotations for the Nullness Checker
 *
 * @author smillst
 */
public class NullnessReflectionResolutionTest {
    @NonNull Object returnNonNull() {
        return new Object();
    }

    void testReturnNonNull(
            @MethodVal(
                        className = "NullnessReflectionResolutionTest",
                        methodName = "returnNonNull",
                        params = 0
                    )
                    Method m)
            throws Exception {
        @NonNull Object o = m.invoke(this);
    }

    void paramNullable(@Nullable Object param1, @Nullable Object param2) {}

    void testParamNullable(
            @MethodVal(
                        className = "NullnessReflectionResolutionTest",
                        methodName = "paramNullable",
                        params = 2
                    )
                    Method m)
            throws Exception {
        @NonNull Object o = m.invoke(this, null, null);
    }

    static @NonNull Object paramAndReturnNonNullStatic(
            @Nullable Object param1, @Nullable Object param2) {
        return new Object();
    }

    void testParamAndReturnNonNullStatic(
            @MethodVal(
                        className = "NullnessReflectionResolutionTest",
                        methodName = "paramAndReturnNonNullStatic",
                        params = 2
                    )
                    Method m)
            throws Exception {
        @NonNull Object o1 = m.invoke(this, null, null);
        @NonNull Object o2 = m.invoke(null, null, null);
    }
}
