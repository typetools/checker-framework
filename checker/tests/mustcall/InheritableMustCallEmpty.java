// A simple test for @InheritableMustCall({}).

import java.io.*;
import org.checkerframework.checker.mustcall.qual.*;

public class InheritableMustCallEmpty {

  @InheritableMustCall({})
  // :: error: inconsistent.mustcall.subtype
  class NoObligationCloseable implements Closeable {
    @Override
    public void close() throws IOException {
      // no resource, nothing to do
    }
  }

  @InheritableMustCall()
  // :: error: inconsistent.mustcall.subtype
  class NoObligationCloseable2 implements Closeable {
    @Override
    public void close() throws IOException {
      // no resource, nothing to do
    }
  }
}
