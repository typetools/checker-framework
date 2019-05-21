import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

@SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
public class PolyConstructor {

    @PolyTainted PolyConstructor() {}

    @PolyTainted PolyConstructor(@PolyTainted Object o) {}

    static void uses(@Tainted Object tainted, @Untainted Object untainted) {
        @Untainted PolyConstructor o1 = new @Untainted PolyConstructor();
        @Tainted PolyConstructor o2 = new @Tainted PolyConstructor();
        @PolyTainted PolyConstructor o3 = new @PolyTainted PolyConstructor();

        // :: error: (assignment.type.incompatible)
        @Untainted PolyConstructor o4 = new @Tainted PolyConstructor(untainted);
        @Untainted PolyConstructor o5 = new PolyConstructor(untainted);

        // This currently isn't supported, but could be in the future.
        // :: error: (assignment.type.incompatible)
        @Untainted PolyConstructor o6 = new PolyConstructor();
        @Tainted PolyConstructor o7 = new PolyConstructor();
    }
}
