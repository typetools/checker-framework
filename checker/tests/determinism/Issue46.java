import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class Issue46 {
    public static void f(List<? extends @PolyDet("use") Object> a) {}
}
