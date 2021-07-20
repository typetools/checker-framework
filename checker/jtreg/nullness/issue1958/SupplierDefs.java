/*
 * @test
 * @summary Test annotations on type parameters in extends.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker SupplierDefs.java
 * @compile/fail/ref=NPE2Test.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker NPE2Test.java -Anomsgtext
 */

import org.checkerframework.checker.nullness.qual.*;

import java.util.function.Supplier;

public class SupplierDefs {
    public abstract static class Supplier<R> {
        public abstract R get();
    }

    public static class NullSupplier extends Supplier<@Nullable String> {
        @Override
        public @Nullable String get() {
            return null;
        }
    }

    public static class NullInterface implements MyInterface<@Nullable String> {
        @Override
        public @Nullable String getT() {
            return null;
        }
    }

    public static class NullSupplierMyInterface extends Supplier<@Nullable String>
            implements MyInterface<@Nullable String> {
        @Override
        public @Nullable String get() {
            return null;
        }

        @Override
        public @Nullable String getT() {
            return null;
        }
    }

    public interface MyInterface<T> {
        T getT();
    }
}
