import org.checkerframework.checker.determinism.qual.*;
import org.checkerframework.framework.qual.*;

public class Issue39 {
    public static void f(@PolyAll int @PolyDet [] arr) {
        arr = new @PolyDet int @PolyDet [0];
    }
}
