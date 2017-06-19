// @below-java8-jdk-skip-test

import org.checkerframework.checker.optional.qual.MaybePresent;
import org.checkerframework.checker.optional.qual.Present;

/** Basic test of subtyping. */
public class SubtypeCheck {

    void foo(@MaybePresent int mp, @Present int p) {
        @MaybePresent int a = mp;
        @MaybePresent int b = p;
        //:: error: assignment.type.incompatible
        @Present int c = mp;
        @Present int d = p;
    }
}
