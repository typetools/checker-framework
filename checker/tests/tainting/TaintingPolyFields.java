import java.util.List;
import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class TaintingPolyFields {
    @PolyTainted Integer x;
    @PolyTainted List<@PolyTainted String> lst;

    // Access of poly fields outside of the declaring class.
    static void test() {
        @Tainted TaintingPolyFields obj = new @Tainted TaintingPolyFields();
        // :: error: (assignment.type.incompatible)
        @Untainted Integer myX = obj.x;
        // :: error: (assignment.type.incompatible)
        @Untainted List<@Untainted String> myLst = obj.lst;
        // :: warning: (cast.unsafe.constructor.invocation)
        @Untainted TaintingPolyFields obj1 = new @Untainted TaintingPolyFields();
        @Untainted Integer myX1 = obj1.x;
        TaintingPolyFields obj2 = new TaintingPolyFields();
        // :: error: (assignment.type.incompatible)
        @Untainted List<@Untainted String> myLst2 = obj2.lst;
    }

    static void polyTest(@PolyTainted TaintingPolyFields o) {
        @PolyTainted Integer f = o.x;
    }
}
