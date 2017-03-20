// Issue 1142 https://github.com/typetools/checker-framework/issues/1142
import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;

class Issue1142 {

    void foo() {
        //:: error: (assignment.type.incompatible)
        ConcurrentHashMap<Integer, @Nullable Integer> chm = new ConcurrentHashMap<>();
        chm.put(1, 1);
        chm.put(3, 3);
        //:: error: (assignment.type.incompatible)
        chm.put(4, null);
    }
}
