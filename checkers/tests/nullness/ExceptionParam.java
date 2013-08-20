package org.junit;

import checkers.nullness.quals.NonNull;
import checkers.nullness.quals.Nullable;
import checkers.quals.DefaultQualifier;

/**
 * Exception parameters are non-null, even if the
 * default is nullable.
 */
@DefaultQualifier(checkers.nullness.quals.Nullable.class)
class ExceptionParam {
    void exc1() {
        try {
        } catch (AssertionError e) {
            @NonNull Object o = e;
        }
    }

    void exc2() {
        try {
        } catch (@NonNull AssertionError e) {
            @NonNull Object o = e;
        }
    }

    void exc3() {
        try {
        //:: error: (type.invalid)
        } catch (@Nullable AssertionError e) {
            @NonNull Object o = e;
        }
    }
}
