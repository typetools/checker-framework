// Test case for https://github.com/typetools/checker-framework/issues/3677
// Without a class-level default, the package-level default applies.

package issue3677;

public class Issue3677PackageDefaultOnly {
  // :: error: (assignment)
  Object field = null;
}
