// RLC-specific stubs mark Throwable.printStackTrace overloads as SideEffectFree so
// that debugging/logging after a resource is closed does not wipe out the close fact.
// This file checks the no-arg, PrintStream, and PrintWriter variants.

import java.io.Closeable;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.Owning;

class ThrowablePrintStackTrace implements Closeable {
  private @Owning CloseableResource resource = new CloseableResource();

  @Override
  @EnsuresCalledMethods(value = "this.resource", methods = "close")
  public void close() {
    resource.close();
    new RuntimeException("debug").printStackTrace();
  }
}

class ThrowablePrintStackTracePrintStream implements Closeable {
  private static final PrintStream STREAM = System.err;

  private @Owning CloseableResource resource = new CloseableResource();

  @Override
  @EnsuresCalledMethods(value = "this.resource", methods = "close")
  public void close() {
    resource.close();
    new RuntimeException("debug").printStackTrace(STREAM);
  }
}

class ThrowablePrintStackTracePrintWriter implements Closeable {
  private static final PrintWriter WRITER = new PrintWriter(new StringWriter());

  private @Owning CloseableResource resource = new CloseableResource();

  @Override
  @EnsuresCalledMethods(value = "this.resource", methods = "close")
  public void close() {
    resource.close();
    new RuntimeException("debug").printStackTrace(WRITER);
  }
}
