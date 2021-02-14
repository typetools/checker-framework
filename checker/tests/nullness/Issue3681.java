// Test case for isse #3681: https://tinyurl.com/cfissue/3681

// @below-java11-jdk-skip-test
package org.jro.tests.checkerfwk.utils;

public class Issue3681 {
    interface PartialFunction<T, R> {
        R apply(T t);

        boolean isDefinedAt(T value);
    }

    interface Either<L, R> {
        R get();

        boolean isRight();
    }

    public static <L, R> PartialFunction<Either<L, R>, R> createKeepRight() {
        return new PartialFunction<>() {

            @Override
            public R apply(final Either<L, R> either) {
                return either.get();
            }

            @Override
            public boolean isDefinedAt(final Either<L, R> value) {
                return value.isRight();
            }
        };
    }

    public static <L, R>
            PartialFunction<Either<L, ? extends R>, ? extends R> createRCovariantKeepRight() {
        return new PartialFunction<>() {

            @Override
            public R apply(final Either<L, ? extends R> either) {
                return either.get();
            }

            @Override
            public boolean isDefinedAt(final Either<L, ? extends R> value) {
                return value.isRight();
            }
        };
    }
}
