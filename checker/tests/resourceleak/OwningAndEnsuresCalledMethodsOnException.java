// Test case for a Resource Leak manual example that involves interaction
// between @Owning and @EnsuresCalledMethodsOnException.

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class OwningAndEnsuresCalledMethodsOnException implements Closeable {

  private final @Owning Closeable resource;

  // Good constructor, as illustrated in the manual.
  @EnsuresCalledMethodsOnException(value = "#1", methods = "close")
  public OwningAndEnsuresCalledMethodsOnException(@Owning Closeable resource) throws IOException {
    this.resource = resource;
    try {
      initialize(resource);
    } catch (Exception e) {
      resource.close();
      throw e;
    }
  }

  // Alternative constructor with a weaker contract (specifically, the default
  // contract for @Owning: only takes ownership on normal return)
  public OwningAndEnsuresCalledMethodsOnException(@Owning Closeable resource, int ignored)
      throws IOException {
    this.resource = resource;
    initialize(resource);
  }

  public OwningAndEnsuresCalledMethodsOnException() throws IOException {
    // OK: the good delegate constructor will either take ownership or close the argument
    // This will issue a false positive warning due to
    // https://github.com/typetools/checker-framework/issues/6270
    // ::error: (required.method.not.called)
    this(new Resource());
  }

  public OwningAndEnsuresCalledMethodsOnException(int x) throws IOException {
    // WRONG: the bad delegate constructor does not close the argument on exception
    // ::error: (required.method.not.called)
    this(new Resource(), x);
  }

  static void exampleUseInNormalMethod1() throws IOException {
    // OK: the constructor will either take ownership or close the argument
    // This will issue a false positive warning due to
    // https://github.com/typetools/checker-framework/issues/6270
    // ::error: (required.method.not.called)
    new OwningAndEnsuresCalledMethodsOnException(new Resource());
  }

  static void exampleUseInNormalMethod2() throws IOException {
    // WRONG: the bad constructor does not close the argument on exception
    // ::error: (required.method.not.called)
    new OwningAndEnsuresCalledMethodsOnException(new Resource(), 0);
  }

  static void initialize(Closeable resource) throws IOException {}

  @EnsuresCalledMethods(value = "resource", methods = "close")
  public void close() throws IOException {
    resource.close();
  }

  private static class Resource implements Closeable {
    @Override
    public void close() throws IOException {}
  }
}
