import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.*;
import java.util.zip.ZipFile;

public class TryWithResources {
    void m1(InputStream stream) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(stream))) {
            in.toString();
        } catch (Exception e) {
        }
    }

    void m2() {
        try (BufferedReader in = null) {
            // :: error: (dereference.of.nullable)
            in.toString();
        } catch (Exception e) {
        }
    }

    // Check that catch blocks and code after try-catch are part of CFG (and flow-sensitive
    // type-refinements work there).
    boolean m3(@Nullable Object x) {
        try (ZipFile f = openZipFile()) {
            return true;
        } catch (IOException e) {
            if (x != null) {
                // OK
                x.toString();
            }
        }

        if (x != null) {
            // OK
            return x.equals(x);
        }

        return false;
    }

    // Helper
    private static ZipFile openZipFile() throws IOException {
        throw new IOException("No zip-file for you!");
    }
}
