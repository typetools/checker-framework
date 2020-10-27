// Test case for isse #3681: https://tinyurl.com/cfissue/3681

// @skip-test until the bug is fixed

package org.jro.tests.checkerfwk.utils;

import org.checkerframework.checker.nullness.qual.NonNull;

public class Issue3681 {
    interface PartialFunction<T, R> {
        R apply(T t);

        boolean isDefinedAt(T value);
    }

    interface Either<L, R> {
        R get();

        boolean isRight();
    }

    public static @NonNull <L, R> PartialFunction<Either<L, R>, R> createKeepRight() {
        return new PartialFunction<>() {

            @Override
            @NonNull public R apply(final @NonNull Either<L, R> either) {
                return either.get();
            }

            @Override
            public boolean isDefinedAt(final @NonNull Either<L, R> value) {
                return value.isRight();
            }
        };
    }

    public static @NonNull <L, R>
            PartialFunction<Either<L, ? extends R>, ? extends R> createRCovariantKeepRight() {
        return new PartialFunction<>() {

            @Override
            @NonNull public R apply(final @NonNull Either<L, ? extends R> either) {
                return either.get();
            }

            @Override
            public boolean isDefinedAt(final @NonNull Either<L, ? extends R> value) {
                return value.isRight();
            }
        };
    }
}
