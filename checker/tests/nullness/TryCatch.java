import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.*;

class EntryReader {
  public EntryReader() throws IOException {}
}

public class TryCatch {
  void constructorException() throws IOException {
    List<Exception> file_errors = new ArrayList<>();
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
      // Note that this code is dead.
      // :: error: (dereference.of.nullable)
      t.toString();
    }
  }

  void noClassDefFoundError(@Nullable Object x) {
    try {
      Class cls = EntryReader.class;
    } catch (NoClassDefFoundError e) {
      if (x != null) {
        // OK
        x.toString();
      }
    }
  }
}
