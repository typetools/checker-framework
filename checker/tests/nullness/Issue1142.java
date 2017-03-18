// Issue 1142 https://github.com/typetools/checker-framework/issues/1142

import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;

class Issue1142 {

  void foo() {

    ConcurrentHashMap<Integer, @Nullable Integer> chm =
        new ConcurrentHashMap<Integer, @Nullable Integer>();
    chm.put(1, 1);
    chm.put(3, 3);
    chm.put(4, null);

  }
}
