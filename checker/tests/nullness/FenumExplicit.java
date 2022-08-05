// Test case for https://github.com/typetools/checker-framework/issues/5165

// @skip-test until the bug is fixed

import org.checkerframework.checker.nullness.qual.Nullable;

class EnumExplicit {

    public static enum EnumWithMethod {
        VALUE {
            @Override
            public void call(@Nullable String string) {
                // Null string is acceptable in this function.
            }
        };

        public abstract void call(String string);
    }

    public static void main(String[] args) {
        EnumWithMethod.VALUE.call(null);
    }
}
