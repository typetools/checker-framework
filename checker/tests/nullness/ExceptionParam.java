package org.junit;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

/** Exception parameters are non-null, even if the default is nullable. */
@DefaultQualifier(org.checkerframework.checker.nullness.qual.Nullable.class)
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
            // :: error: (type.invalid.annotations.on.use)
        } catch (@Nullable AssertionError e) {
            @NonNull Object o = e;
        }
    }
}
