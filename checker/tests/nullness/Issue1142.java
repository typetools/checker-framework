// Issue 1142 https://github.com/typetools/checker-framework/issues/1142
import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;

class Issue1142 {

    void foo() {
        //error expected on assignment compatibility
        ConcurrentHashMap<Integer, @Nullable Integer> chm1 = new ConcurrentHashMap<>();

        //error expected on assignment compatibility
        chm1.put(1, null);
    }
}
