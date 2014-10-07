import java.io.*;

import org.checkerframework.checker.nullness.qual.*;

public class CallSuper extends FilterInputStream {
  CallSuper(@Nullable InputStream in) {
    // The FilterInputStream constructor takes a NonNull argument.
    //:: error: (argument.type.incompatible)
    super(in);
  }
}
