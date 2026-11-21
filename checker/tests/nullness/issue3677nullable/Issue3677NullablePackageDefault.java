// Test case for https://github.com/typetools/checker-framework/issues/3677
// Without a class-level default, the package-level default applies.  The package default
// (Nullable) differs from the fallback default (NonNull), so this test fails if the package
// default is ignored.

package issue3677nullable;

public class Issue3677NullablePackageDefault {
  Object field = null;
}
