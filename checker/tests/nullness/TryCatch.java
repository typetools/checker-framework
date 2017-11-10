import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.*;

class EntryReader {
    public EntryReader() throws IOException {}
}

class TryCatch {
    void constructorException() throws IOException {
        List<Exception> file_errors = new ArrayList<Exception>();
        try {
            new EntryReader();
        } catch (FileNotFoundException e) {
            file_errors.add(e);
        }
    }

    void unreachableCatch(String[] xs) {
        String t = "";
        t.toString();
        try {
        } catch (Throwable e) {
            // :: error: (dereference.of.nullable)
            t.toString();
        }
    }
}
