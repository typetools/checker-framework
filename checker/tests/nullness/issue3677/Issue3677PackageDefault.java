// Test case for https://github.com/typetools/checker-framework/issues/3677
// A class-level default takes precedence over a package-level default.

package issue3677;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.FIELD)
public class Issue3677PackageDefault {
  Object field = null;
}
