import org.checkerframework.checker.signedness.qual.PolySigned;
import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;

import java.util.Date;

public class RestrictedPolymorphism {

    @Signed Date sd;
    @Unsigned Date ud;

    public void foo(@PolySigned Object a, @PolySigned Object b) {}

    void client() {
        foo(sd, sd);
        // :: error: (argument.type.incompatible)
        foo(sd, ud);
        // :: error: (argument.type.incompatible)
        foo(ud, sd);
        foo(ud, ud);
    }
}
