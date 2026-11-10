// Test case for https://github.com/typetools/checker-framework/issues/3677

@DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.ALL)
package issue3677;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
