import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

class Issue1000 {
    //:: error: illegal.instantiation
    void illegalInstantiation(Optional<@Nullable String> arg) {}

    String foo1(Optional<String> opt) {
        return opt.orElse("");
    }

    String foo2(Optional<String> opt) {
        //:: error: invalid.return.type
        return opt.orElse(null);
    }
}
