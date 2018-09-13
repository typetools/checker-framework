import java.util.Set;
import org.checkerframework.checker.determinism.qual.*;

public class TestPolyReplace {
    <T> void testPolyDown(@OrderNonDet Set<T> set, @Det T elem) {
        @Det boolean out = set.contains(elem);
    }
}
