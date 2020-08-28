package withdefault;

import org.checkerframework.checker.tainting.qual.PolyTainted;

public class WithDefault {
    @PolyTainted int field;
}
