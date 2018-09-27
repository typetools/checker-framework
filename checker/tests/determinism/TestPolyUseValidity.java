import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestPolyUseValidity {
    // :: error: (invalid.polydet.use)
    @PolyDet("use") int invalidReturn(@PolyDet int a) {
        return a;
    }

    void invalidLocal(@PolyDet int a) {
        // :: error: (invalid.polydet.use)
        @PolyDet("use") int local;
        // :: error: (invalid.polydet.use)
        @PolyDet int @PolyDet("use") [] local1;
    }

    @Det int validParam(@PolyDet int a, @PolyDet("use") int b) {
        return 5;
    }

    <T extends @PolyDet("use") Object> void validUpperBound(@PolyDet List<T> list) {}

    void validTypevar(@PolyDet List<@PolyDet("use") String> list) {}

    void validLocal(@PolyDet int a, @PolyDet("use") int b) {
        int x = b;
    }

    <@PolyDet("use") T> void validArray(T @PolyDet [] @PolyDet [] arr) {}

    // :: error: (invalid.polydet.use)
    class InnerClass<T extends @PolyDet("use") Object> {}

    // :: error: (invalid.polydet.use)
    class InnerClass1<@PolyDet("use") T> {}

    // :: error: (invalid.polydet.use)
    class InnerClass2<@PolyDet("use") Integer> {}
}
