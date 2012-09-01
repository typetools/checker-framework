import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier(NonNull.class)
public class DefaultFlow {

    void test() {

        @Nullable String reader = null;
        if (reader == null)
            return;

        reader.startsWith("hello");
    }

    void tesVariableInitialization() {
        @Nullable Object elts = null;
        assert elts != null: "nullness";
        @NonNull Object elem = elts;
    }
}
