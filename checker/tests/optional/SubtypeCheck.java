import org.checkerframework.checker.optional.qual.MaybePresent;
import org.checkerframework.checker.optional.qual.Present;

/** Basic test of subtyping. */
public class SubtypeCheck {

    void foo(@MaybePresent int mp, @Present int p) {
        @MaybePresent int mp2 = mp;
        @MaybePresent int mp3 = p;
        // :: error: assignment.type.incompatible
        @Present int p2 = mp;
        @Present int p3 = p;
    }
}
