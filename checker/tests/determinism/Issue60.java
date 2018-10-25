import org.checkerframework.checker.determinism.qual.*;

public class Issue60 {
    public static void f(@PolyDet int @PolyDet [] a) {
        // :: error: (assignment.type.incompatible)
        @Det int len = a.length;
    }
}
