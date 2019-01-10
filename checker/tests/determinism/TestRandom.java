import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestRandom {
    void testSetSeed(@Det Random r, @NonDet int seed) {
        // :: error: (argument.type.incompatible)
        r.setSeed(seed);
    }

    void testNextBytes(@NonDet Random r, @Det byte @Det [] dest) {
        // :: error: (argument.type.incompatible)
        r.nextBytes(dest);
    }
}
