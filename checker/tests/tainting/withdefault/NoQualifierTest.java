package withdefault;

import org.checkerframework.checker.tainting.qual.*;
import org.checkerframework.framework.qual.*;

@NoQualifierParameter(Tainted.class)
public class NoQualifierTest {
    // :: error: (invalid.polymorphic.qualifier.use)
    @PolyTainted int field;
}
