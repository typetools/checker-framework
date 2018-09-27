import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class ArrayTypes {
    public static void f(@PolyDet Object @PolyDet [] arr) {}

    public static void g() {
        @PolyDet Object @PolyDet [] arr = (new ArrayList<String>()).toArray();
        // :: error: (invalid.array.component.type)
        f(arr);
    }

    public static void g1(@PolyDet Object @PolyDet [] arr) {
        f(arr);
    }
}
