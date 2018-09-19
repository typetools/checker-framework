import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestPolyHierarchy {
    @PolyDet("down") Set<@Det Integer> checkHierarchy(@PolyDet Set<@Det Integer> set) {
        @PolyDet("up") Set<@Det Integer> local = (@PolyDet("up") Set<@Det Integer>) set;
        return local;
    }
}
