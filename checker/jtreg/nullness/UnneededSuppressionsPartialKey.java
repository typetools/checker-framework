/*
 * @test
 * @summary Test that a partial message key does not suppress an "unneeded.suppression" warning
 *
 * @compile/ref=UnneededSuppressionsPartialKey.goal -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AwarnUnneededSuppressions UnneededSuppressionsPartialKey.java
 */

// In each method, the "nullness:return" suppresses nothing, so the checker issues an
// "unneeded.suppression" warning about the "nullness:return".  The second string in each
// annotation determines whether that warning is itself suppressed.

class UnneededSuppressionsPartialKey {

  // The complete message key suppresses the warning.

  @SuppressWarnings({"nullness:return", "unneeded.suppression"})
  public String completeKey() {
    return "hello";
  }

  @SuppressWarnings({"nullness:return", "nullness:unneeded.suppression"})
  public String completeKeyWithPrefix() {
    return "hello";
  }

  // A partial message key does not suppress the warning.

  @SuppressWarnings({"nullness:return", "suppression"})
  public String partialKeySuffix() {
    return "hello";
  }

  @SuppressWarnings({"nullness:return", "unneeded"})
  public String partialKeyPrefix() {
    return "hello";
  }

  @SuppressWarnings({"nullness:return", "nullness:suppression"})
  public String partialKeySuffixWithPrefix() {
    return "hello";
  }
}
