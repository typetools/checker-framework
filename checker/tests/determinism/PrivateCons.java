import org.checkerframework.checker.determinism.qual.*;

// @skip-test
public class PrivateCons {
    private PrivateCons() {}

    public static void meth() {}

    public static void callMeth() {
        meth();
    }
}
