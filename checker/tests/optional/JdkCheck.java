import java.util.Optional;
import java.util.function.Supplier;
import org.checkerframework.checker.optional.qual.MaybePresent;
import org.checkerframework.checker.optional.qual.Present;

public class JdkCheck {

    boolean isPresentTest1(@Present Optional<String> pos) {
        return pos.isPresent();
    }

    boolean isPresentTest2(@MaybePresent Optional<String> mos) {
        return mos.isPresent();
    }

    String orElseThrowTest1(
            @Present Optional<String> pos, Supplier<RuntimeException> exceptionSupplier) {
        return pos.orElseThrow(exceptionSupplier);
    }

    String orElseThrowTest2(
            @MaybePresent Optional<String> mos, Supplier<RuntimeException> exceptionSupplier) {
        //:: error: method.invocation.invalid
        return mos.orElseThrow(exceptionSupplier);
    }

    String getTest1(@Present Optional<String> pos) {
        return pos.get();
    }

    String getTest2(@MaybePresent Optional<String> mos) {
        //:: error: method.invocation.invalid
        return mos.get();
    }

    @Present Optional<String> ofTest1(String s) {
        return Optional.of(s);
    }

    @MaybePresent Optional<String> ofTest2(String s) {
        return Optional.of(s);
    }

    @Present Optional<String> ofNullableTest1(String s) {
        //:: error: return.type.incompatible
        return Optional.ofNullable(s);
    }

    @MaybePresent Optional<String> ofNullableTest2(String s) {
        return Optional.ofNullable(s);
    }
}
