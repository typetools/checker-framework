// Test case for https://github.com/typetools/checker-framework/issues/5908

import org.checkerframework.checker.mustcall.qual.*;

class Resource implements java.io.Closeable {
  @Override
  public void close() {}
}

public class SneakyDrop {

  public static <T> void sneakyDrop(@Owning T value) {}

  public static void main(String[] args) throws Exception {
    Resource x = new Resource();
    // :: error: type.argument
    sneakyDrop(x);
  }

  // :: error: required.method.not.called
  public static <T> void sneakyDrop2(@Owning @MustCall("close") T value) {}

  public static void main2(String[] args) throws Exception {
    Resource x = new Resource();
    sneakyDrop2(x);
  }

  // :: error: (required.method.not.called)
  public static <T extends @MustCall("close") Object> void sneakyDrop3(@Owning T value) {}

  public static void main3(String[] args) throws Exception {
    Resource x = new Resource();
    sneakyDrop3(x);
  }

  public static <T extends Object> void sneakyDrop4(@Owning T value) {}

  public static void main4(String[] args) throws Exception {
    Resource x = new Resource();
    // :: error: type.argument
    sneakyDrop4(x);
  }

  // :: error: (required.method.not.called)
  public static <T extends java.io.Closeable> void sneakyDrop5(@Owning T value) {}

  public static void main5(String[] args) throws Exception {
    Resource x = new Resource();
    sneakyDrop5(x);
  }

  public static <T extends java.io.Closeable> void sneakyDropCorrect(
      @Owning @MustCall("close") T value) throws Exception {
    value.close();
  }

  public static void main6(String[] args) throws Exception {
    Resource x = new Resource();
    try {
      sneakyDropCorrect(x);
    } catch (Exception e) {
      x.close();
    }
  }
}
