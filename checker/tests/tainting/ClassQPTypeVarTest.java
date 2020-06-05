import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.framework.qual.HasQualifierParameter;

public class ClassQPTypeVarTest {
    @HasQualifierParameter(Tainted.class)
    interface Buffer {
        void append(@PolyTainted String s);
    }

    <T> @Tainted T cast(T param) {
        return param;
    }

    void bug(@Untainted Buffer b, @Tainted String s) {
        // :: error: (argument.type.incompatible)
        b.append(s);
        // :: error: (type.argument.invalid.hasqualparam)
        cast(b).append(s);
    }
}
