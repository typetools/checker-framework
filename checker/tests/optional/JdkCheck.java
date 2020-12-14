import java.util.Optional;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.optional.qual.Present;

/** Test JDK annotations. */
@SuppressWarnings("optional.parameter")
public class JdkCheck {

    boolean isPresentTest1(@Present Optional<String> pos) {
        return pos.isPresent();
    }

    boolean isPresentTest2(Optional<String> mos) {
        return mos.isPresent();
    }

    String orElseThrowTest1(
            @Present Optional<String> pos, Supplier<RuntimeException> exceptionSupplier) {
        return pos.orElseThrow(exceptionSupplier);
    }

    String orElseThrowTest2(Optional<String> mos, Supplier<RuntimeException> exceptionSupplier) {
        // :: error: (method.invocation.invalid)
        return mos.orElseThrow(exceptionSupplier);
    }

    String orElseThrowTestFlow(Optional<String> mos, Supplier<RuntimeException> exceptionSupplier) {
        // :: error: (method.invocation.invalid)
        mos.orElseThrow(exceptionSupplier);
        return mos.get();
    }

    String getTest1(@Present Optional<String> pos) {
        return pos.get();
    }

    String getTest2(Optional<String> mos) {
        // :: error: (method.invocation.invalid)
        return mos.get();
    }

    @Present Optional<String> ofTestPNn(String s) {
        return Optional.of(s);
    }

    Optional<String> ofTestMNn(String s) {
        return Optional.of(s);
    }

    @Present Optional<String> ofTestPNble(@Nullable String s) {
        // TODO :: error: (of.nullable.argument) :: error: (return.type.incompatible)
        return Optional.of(s);
    }

    Optional<String> ofTestMNble(@Nullable String s) {
        // TODO :: error: (of.nullable.argument) :: error: (return.type.incompatible)
        return Optional.of(s);
    }

    @Present Optional<String> ofNullableTestPNble(@Nullable String s) {
        // :: error: (return.type.incompatible)
        return Optional.ofNullable(s);
    }

    /* TODO: ofNullable with non-null arg gives @Present (+ a warning?)
    @Present Optional<String> ofNullableTestPNn(String s) {
        return Optional.ofNullable(s);
    }
    */

    Optional<String> ofNullableTestMNble(@Nullable String s) {
        return Optional.ofNullable(s);
    }

    Optional<String> ofNullableTestMNn(String s) {
        return Optional.ofNullable(s);
    }
}
