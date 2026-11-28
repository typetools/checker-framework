// Test case for https://github.com/typetools/checker-framework/issues/3677

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

@DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.FIELD)
@DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.PARAMETER)
@DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.RETURN)
public class Issue3677 {

  // :: error: (assignment)
  Issue3677 pattern = null;

  @DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.ALL)
  class Inner {
    Issue3677 sql2003 = null;
  }
}
