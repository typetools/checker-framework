import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

public class Uninit8 {

    Object f;

    Uninit8() {
        setFields();
        f.toString();
    }

    @EnsuresNonNull("f")
    void setFields(@UnknownInitialization Uninit8 this) {
        f = new Object();
    }
}
