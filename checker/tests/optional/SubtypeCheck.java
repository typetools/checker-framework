import org.checkerframework.checker.optional.qual.*;

public class SubtypeCheck {

    void foo(@MaybePresent int mp, @Present int p) {
        @MaybePresent int a = mp;
        @MaybePresent int b = p;
        @Present int c = mp; // expected error on this line
        @Present int d = p;
    }
}
