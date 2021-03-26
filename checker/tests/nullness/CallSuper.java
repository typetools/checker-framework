import java.io.*;
import org.checkerframework.checker.nullness.qual.*;

class MyFilterInputStream {
  MyFilterInputStream(InputStream in) {}
}

public class CallSuper extends MyFilterInputStream {
  CallSuper(@Nullable InputStream in) {
    // The MyFilterInputStream constructor takes a NonNull argument
    // (but that's not true of FilterInputStream itself).
    // :: error: (argument.type.incompatible)
    super(in);
  }
}
