// Test case that was submitted in Issue 404, but was combined with Issue 979
// https://github.com/typetools/checker-framework/issues/979
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public final class Issue404 {
  public Set<String> uniqueTrimmed(final Collection<String> strings) {
    return strings.stream().map(String::trim).collect(Collectors.toSet());
  }
}
