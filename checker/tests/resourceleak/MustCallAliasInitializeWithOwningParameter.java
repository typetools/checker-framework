// Test that methods like `allocateAndInitializeService` are accepted by
// the resource leak checker.

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

public class MustCallAliasInitializeWithOwningParameter {

  public Service allocateAndInitializeService(@Owning Closeable resource) throws IOException {
    Service service = new Service(resource);
    service.initialize();
    return service;
  }

  private static class Service implements Closeable {

    private final @Owning Closeable wrappedResource;

    public @MustCallAlias Service(@MustCallAlias Closeable resource) {
      this.wrappedResource = resource;
    }

    public void initialize() throws IOException {}

    @Override
    @EnsuresCalledMethods(value = "wrappedResource", methods = "close")
    public void close() throws IOException {
      wrappedResource.close();
    }
  }
}
