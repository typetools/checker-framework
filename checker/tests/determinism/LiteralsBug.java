import org.checkerframework.checker.determinism.qual.*;

public class LiteralsBug {
    public static void f() {
        int @PolyDet("up") [] arr;
    }
}
