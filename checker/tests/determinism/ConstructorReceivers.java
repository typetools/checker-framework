import org.checkerframework.checker.determinism.qual.*;

public class ConstructorReceivers<T extends @NonDet Object> {
    //    public static void f(@PolyDet int a) {
    //        ConstructorReceivers<@PolyDet Integer> b = new ConstructorReceivers<@PolyDet
    // Integer>();
    //    }
    public static void f() {
        @Det ConstructorReceivers<@NonDet Integer> b = new ConstructorReceivers<@Det Integer>();
    }
}
