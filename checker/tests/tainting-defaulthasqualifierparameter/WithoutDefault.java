import org.checkerframework.checker.tainting.qual.*;
import org.checkerframework.framework.qual.*;

public class WithoutDefault {
    // :: error: (invalid.polymorphic.qualifier.use)
    @PolyTainted int field;
}
