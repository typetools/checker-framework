// Testcase for Issue 741
// https://github.com/typetools/checker-framework/pull/741
// @skip-test
public class Issue741 {
  @SuppressWarnings("unchecked")
  public <T> T incompatibleTypes(Object o) {
    final T x = (T) o;
    if (x != null) {}
    // invalid error here
    return x;
  }

  @SuppressWarnings("unchecked")
  public <T> T noIncompatibleTypes(Object o) {
    final T x = (T) o;
    // no error here
    return x;
  }
}
