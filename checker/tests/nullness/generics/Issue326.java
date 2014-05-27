import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;
import java.util.HashSet;

class Issue326 {
    {
        Set<@Nullable String> local = new HashSet<>();
    }

    Set<@Nullable String> field = new HashSet<>();
}
