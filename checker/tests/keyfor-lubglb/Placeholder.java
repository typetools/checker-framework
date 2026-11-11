import org.checkerframework.checker.nullness.qual.KeyFor;

// We need a file to start the checker.
// KeyForLubGlbChecker checks that the noncanonical annotation on this field is canonicalized.

public class Placeholder {
  @KeyFor({"b", "a"}) String noncanonicalKeyFor;
}
