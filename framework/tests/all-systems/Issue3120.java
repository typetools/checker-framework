@SuppressWarnings("all") // Ensure no crashes
public class Issue3120 {
    CharSequence foo() {
        return bar();
    }

    <T extends Enum<T>> CharSequence bar() {
        return null;
    }

    CharSequence foo0() {
        return bar0(null);
    }

    <EnumT extends Enum<EnumT> & AnotherType> CharSequence bar0(SomeType<?> type) {
        return null;
    }

    class SomeType<T> {}

    interface AnotherType {}
}
