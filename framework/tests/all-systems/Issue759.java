// Testcase for Issue759
// https://github.com/typetools/checker-framework/issues/759
// @skip-test
public class Issue759 {
    void possibleValues(final Class<? extends Enum> enumType) {
        //:: warning: [unchecked] unchecked method invocation: method lowercase in class Issue759 is applied to given types
        lowercase(enumType.getEnumConstants());
        //:: warning: [unchecked] unchecked method invocation: method lowercase2 in class Issue759 is applied to given types
        lowercase2(enumType.getEnumConstants());
        lowercase3(enumType.getEnumConstants());
    }

    //:: warning: [unchecked] Possible heap pollution from parameterized vararg type T
    <T extends Enum<T>> void lowercase(final T... items) {
    }

    <T extends Enum<T>> void lowercase2(final T[] items) {
    }

    <T> void lowercase3(final T items) {
    }
}

class Gen<T extends Gen<T>> {
    T[] getConstants() { return null; }
}

class IncompatibleTypes {
    void possibleValues(final Gen<?> genType) {
        lowercase(genType.getConstants());
    }

    <S> void lowercase(final S items) {
    }
}
