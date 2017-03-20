// Issue 1142 https://github.com/typetools/checker-framework/issues/1142
import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;

class Issue1142 {

    void foo() {

        ConcurrentHashMap<Integer, Integer> chm1 = new ConcurrentHashMap<>();

        ConcurrentHashMap<Integer, @Nullable Integer> chm2 = new ConcurrentHashMap<>();

        //:: error: (assignment.type.incompatible)
        chm1.put(1, null);
        chm2.put(1, null);
    }
}
