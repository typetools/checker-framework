// Test case for Issue 1307
// https://github.com/typetools/checker-framework/issues/1307

import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.*;

@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.FIELD)
@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.PARAMETER)
class Issue1307 {
    Object nullableField = null;

    void perl(Integer a) {
        a = null;
    }
}
