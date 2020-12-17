// Test case for issue #243: https://github.com/typetools/checker-framework/issues/243

public class TestValOf<T extends Enum<T>> {

    private final Class<T> enumClass;

    private TestValOf(Class<T> enumClass) {
        this.enumClass = enumClass;
    }

    T foo(String value) {
        return Enum.valueOf(enumClass, value);
    }
}
