import java.util.Collection;
import org.checkerframework.checker.nullness.qual.Nullable;

class ContainsAllRetainAll {
  void bulkOperations(Collection<String> a, Collection<@Nullable String> b) {
    a.containsAll(b);
  }
}
