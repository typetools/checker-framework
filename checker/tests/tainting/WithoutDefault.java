import org.checkerframework.checker.tainting.qual.PolyTainted;

public class WithoutDefault {
    // :: error: (invalid.polymorphic.qualifier.use)
    @PolyTainted int field;
}
