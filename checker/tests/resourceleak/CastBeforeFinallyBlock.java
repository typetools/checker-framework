import java.io.Closeable;

public abstract class CastBeforeFinallyBlock {

  protected abstract Closeable alloc() throws Exception;

  protected abstract Object getInt() throws Exception;

  public void f() throws Exception {
    // Previous versions of the resource leak checker reported a false positive
    // for this code ("close may not have been invoked on y").  However, this
    // code is correct.

    Integer x = null;
    try {
      try (Closeable y = alloc()) {
        System.out.println(y);
      }
      x = (Integer) getInt();
    } finally {
      System.out.println(x);
    }
  }
}
