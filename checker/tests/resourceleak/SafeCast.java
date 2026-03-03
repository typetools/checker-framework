// simple test that the Resource Leak Checker can track data flow through a downcast
import java.io.Closeable;
import java.io.InputStream;

public abstract class SafeCast {

  protected abstract Closeable alloc() throws Exception;

  public void f() throws Exception {
    InputStream s = (InputStream) alloc();
    s.close();
  }
}
