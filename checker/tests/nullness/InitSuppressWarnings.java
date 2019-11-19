import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

public class InitSuppressWarnings {

    private void init_vars(@UnderInitialization(Object.class) InitSuppressWarnings this) {
        // "initialization" should work as a key to suppress this warning.
        @SuppressWarnings({"initialization"})
        @Initialized InitSuppressWarnings initializedThis = this;
    }
}
