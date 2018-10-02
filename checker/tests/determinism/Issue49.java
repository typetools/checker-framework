import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class Issue49 {
    public static @Det int f(@NonDet boolean b, @Det int i, @Det int j) {
        // :: error: (invalid.type.on.conditional.expression)
        return (b) ? i : j;
    }
}
