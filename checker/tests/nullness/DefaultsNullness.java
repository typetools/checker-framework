import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

public class DefaultsNullness {

    // local variable defaults
    void test(@UnknownInitialization DefaultsNullness para, @Initialized DefaultsNullness comm) {
        // @Nullable @UnknownInitialization by default
        String s = "abc";

        s = null;

        DefaultsNullness d;
        d = null; // null okay (default == @Nullable)

        d = comm; // committed okay (default == @Initialized)
        d.hashCode();
    }
}
