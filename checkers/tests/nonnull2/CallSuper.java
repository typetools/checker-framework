import java.io.*;

import checkers.nullness.quals.*;

public class CallSuper extends FilterInputStream {
  CallSuper(@Nullable InputStream in) {
    // The FilterInputStream constructor takes a NonNull argument.
    //:: error: (argument.type.incompatible)
    super(in);
  }
}
