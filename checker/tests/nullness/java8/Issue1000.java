import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

class Issue1000 {
    //:: error: (type.argument.type.incompatible
    void illegalInstantiation(Optional<@Nullable String> arg) {}

    String orElseAppliedToNonNull(Optional<String> opt) {
        return opt.orElse("");
    }

    String orElseAppliedToNullable(Optional<String> opt) {
        //:: error: (return.type.incompatible)
        return opt.orElse(null);
    }
}
