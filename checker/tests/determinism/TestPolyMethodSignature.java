import java.util.List;
import org.checkerframework.checker.determinism.qual.*;

public class TestPolyMethodSignature {
    // :: error: (invalid.polydet.up)
    static void testPolyUpInvalid(@PolyDet("up") Integer a) {}

    void testPolyUpValid(@PolyDet TestPolyMethodSignature this, @PolyDet("up") Integer a) {}

    void testPolyUpValid1(@PolyDet("up") Integer a) {}

    static @PolyDet("up") int checkListValid(@PolyDet List<@PolyDet Integer> lst) {
        return 0;
    }

    // :: error: (invalid.polydet.up)
    static @PolyDet("up") int checkListInvalid(@PolyDet("up") List<@PolyDet Integer> lst) {
        return 0;
    }

    // :: error: (invalid.polydet.up)
    static <T extends @PolyDet Object> @PolyDet("down") int checkListInvalid2(
            @PolyDet("up") List<T> lst) {
        return 0;
    }
}
