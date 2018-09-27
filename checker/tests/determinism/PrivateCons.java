import org.checkerframework.checker.determinism.qual.*;

public class PrivateCons {
    private PrivateCons() {}

    public static void meth() {}

    public static void callMeth() {
        meth();
    }
}
