import java.util.Optional;
import org.checkerframework.checker.optional.qual.MaybePresent;
import org.checkerframework.checker.optional.qual.Present;

/** Basic test of subtyping. */
public class SubtypeCheck {

    @SuppressWarnings("optional.parameter")
    void foo(@MaybePresent Optional<String> mp, @Present Optional<String> p) {
        @MaybePresent Optional<String> mp2 = mp;
        @MaybePresent Optional<String> mp3 = p;
        // :: error: assignment.type.incompatible
        @Present Optional<String> p2 = mp;
        @Present Optional<String> p3 = p;
    }
}
