import org.checkerframework.checker.mustcall.qual.Owning;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;

public class StaticOwningFieldOtherClass {}

abstract class HasStaticOwningField {
    // :: error: (required.method.not.called)
    public static @Owning FileWriter log = null;
}

class TestUtils {
    // :: error: (missing.creates.mustcall.for)
    public static void setLog(String filename) {
        try {
            // :: error: (required.method.not.called)
            HasStaticOwningField.log = new FileWriter(filename);
        } catch (IOException ioe) {
            throw new UncheckedIOException("Cannot write file " + filename, ioe);
        }
    }
}
