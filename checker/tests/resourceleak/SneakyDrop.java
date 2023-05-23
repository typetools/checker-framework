// Test case for https://github.com/typetools/checker-framework/issues/5908

import org.checkerframework.checker.mustcall.qual.*;

class Resource implements java.io.Closeable {
  @Override
  public void close() {}
}

public class SneakyDrop {

  // :: error: required.method.not.called
  public static <T> void sneakyDrop(@Owning T value) {}

  public static void main(String[] args) throws Exception {
    Resource x = new Resource();
    sneakyDrop(x);
  }
}
