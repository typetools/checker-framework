import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class Issue50 {
    public static <T extends @PolyDet("use") Object> @PolyDet List<T> f(@PolyDet List<T> list) {
        return null;
    }

    public static <T extends @PolyDet("use") Object> @PolyDet List<T> g(@PolyDet List<T> list) {
        return f(list);
    }
}
