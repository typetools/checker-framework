import java.io.Closeable;
import java.io.IOException;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.Owning;

public class CloseSuper {

  public static class A implements Closeable {
    private final @Owning Closeable resource;

    public A(@Owning Closeable resource) {
      this.resource = resource;
    }

    @Override
    @EnsuresCalledMethods(
        value = "resource",
        methods = {"close"})
    public void close() throws IOException {
      resource.close();
    }
  }

  public static class B extends A {
    public B(@Owning Closeable resource) {
      super(resource);
    }

    @Override
    @EnsuresCalledMethods(
        value = "resource",
        methods = {"close"})
    public void close() throws IOException {
      super.close();
    }
  }
}
