import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue326 {
  {
    Set<@Nullable String> local = new HashSet<>();
  }

  Set<@Nullable String> field = new HashSet<>();
}
