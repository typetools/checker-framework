// Testcase for Issue759
// https://github.com/typetools/checker-framework/issues/759
// Also, see framework/tests/all-systems/Issue759.java

import org.checkerframework.checker.nullness.qual.*;

@SuppressWarnings("unchecked")
public class Issue759 {
    void possibleValues(final Class<? extends Enum> enumType) {
        lowercase(enumType.getEnumConstants());
        lowercase2(enumType.getEnumConstants());
        lowercase3(enumType.getEnumConstants());
    }

    <T extends Enum<T>> void lowercase(final T @Nullable ... items) {}

    <T extends Enum<T>> void lowercase2(final T @Nullable [] items) {}

    <T> void lowercase3(final T items) {}
}

interface Gen<T extends Gen<T>> {
    T[] getConstants();

    T @Nullable [] getNullableConstants();
}

class IncompatibleTypes {
    void possibleValues(final Gen<?> genType) {
        lowercase(genType.getConstants());
        lowercase(genType.getNullableConstants());
    }

    <S> void lowercase(final S items) {}

    void possibleValues2(final Gen<?> genType) {
        lowercase2(genType.getConstants());
        // :: error: (type.argument.type.incompatible)
        lowercase2(genType.getNullableConstants());
    }

    <S extends Object> void lowercase2(final S items) {}

    void possibleValues3(final Gen<?> genType) {
        lowercase3(genType.getConstants());
        // :: error: (argument.type.incompatible)
        lowercase3(genType.getNullableConstants());
    }

    <S> void lowercase3(final @NonNull S items) {}
}
