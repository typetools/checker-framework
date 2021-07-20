import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashSet;
import java.util.Set;

public class Issue326 {
    {
        Set<@Nullable String> local = new HashSet<>();
    }

    Set<@Nullable String> field = new HashSet<>();
}
