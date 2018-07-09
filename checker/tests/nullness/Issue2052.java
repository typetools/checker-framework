import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class Issue2052 {
    public static class ParentW<S> {
        protected final String field;

        public ParentW() {
            // Initializing "field" at the declaration, did not trigger the bug.
            field = "";
        }
    }

    public static class ChildW extends ParentW<String> {
        public String getField(@UnknownInitialization(ParentW.class) ChildW this) {
            return this.field;
        }
    }
}
