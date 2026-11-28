// Test case for https://github.com/typetools/checker-framework/issues/3677
// A package-level default that differs from the checked-code fallback default (NonNull).

@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.FIELD)
package issue3677nullable;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
