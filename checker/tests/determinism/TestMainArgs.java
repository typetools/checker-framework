import java.io.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestMainArgs {
    public static void main(String[] args) {
        @Det String @Det [] a = args;
        System.out.println(a);
    }

    void callMain(@PolyDet String @PolyDet [] ar) {
        // :: error: (argument.type.incompatible)
        main(ar);
    }
}
