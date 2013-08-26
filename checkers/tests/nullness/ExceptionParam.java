package org.junit;

import checkers.nullness.quals.NonNull;
import checkers.quals.DefaultQualifier;

/**
 * Exception parameters are non-null, even if the
 * default is nullable.
 */
@DefaultQualifier(checkers.nullness.quals.Nullable.class)
class ExceptionParam {
    void exc() {
        try {
        } catch (AssertionError e) {
            @NonNull Object o = e;
        }
    }
}
