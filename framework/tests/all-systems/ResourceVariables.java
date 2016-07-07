import java.io.*;
import org.checkerframework.framework.qual.*;

// Tests related to resource variables in try-with-resources statements.

class ResourceVariables {
    void foo(InputStream arg) {
        try (InputStream in = arg) {
        } catch (IOException e) {
        }
    }
}
