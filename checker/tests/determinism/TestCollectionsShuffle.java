import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestCollectionsShuffle {
    void testShuffle(@Det List<@Det Integer> list) {
        Collections.shuffle(list);
        @Det List<@Det Integer> temp = list;
    }
}
