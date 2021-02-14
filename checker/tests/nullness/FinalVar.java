import org.checkerframework.checker.nullness.qual.NonNull;

public class FinalVar {

    public Object pptIterator() {
        // Only test with (effectively) final variables; Java only permits final or
        // effectively final variables to be accessed from an anonymous class.
        final String iter_view_1 = "I am not null";
        @NonNull String iter_view_2 = "Neither am I";
        final @NonNull String iter_view_3 = "Dittos";
        return new Object() {
            public void useFinalVar() {
                iter_view_1.hashCode();
                iter_view_2.hashCode();
                iter_view_3.hashCode();
            }
        };
    }
}
