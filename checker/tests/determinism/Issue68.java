import org.checkerframework.checker.determinism.qual.*;

public class Issue68 {
    @PolyDet Issue68(@PolyDet int i) {}

    static void f(@NonDet int i) {
        // :: warning: (cast.unsafe.constructor.invocation)
        @Det Issue68 p = new @Det Issue68(i);
    }
}
