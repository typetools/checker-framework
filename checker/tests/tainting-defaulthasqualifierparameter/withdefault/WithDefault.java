package withdefault;

import org.checkerframework.checker.tainting.qual.*;
import org.checkerframework.framework.qual.*;

public class WithDefault {
    @PolyTainted int field;
}
