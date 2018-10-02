import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class Issue49 {
    public static @Det int f(@NonDet boolean b, @Det int i, @Det int j) {
        // :: error: (invalid.type.on.conditional)
        return (b) ? i : j;
    }

    public static @Det int f1(@NonDet boolean b, @Det int i, @Det int j) {
        // :: error: (invalid.type.on.conditional)
        if (b) {
            return i;
        }
        return j;
    }
}
