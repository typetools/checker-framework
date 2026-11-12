import java.util.Map;
import org.checkerframework.checker.nullness.qual.KeyFor;

// We need a file to start the checker.
// KeyForLubGlbChecker checks that the noncanonical annotation on field noncanonicalKeyFor is
// canonicalized.

public class Placeholder {
  Map<String, String> a;
  Map<String, String> b;
  @KeyFor({"b", "a"}) String noncanonicalKeyFor;
}
