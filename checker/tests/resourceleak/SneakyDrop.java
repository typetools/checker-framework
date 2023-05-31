// Test case for https://github.com/typetools/checker-framework/issues/5908

import org.checkerframework.checker.mustcall.qual.*;

class Resource implements java.io.Closeable {
  @Override
  public void close() {}
}

public class SneakyDrop {

  // :: error: required.method.not.known
  public static <T> void sneakyDrop(@Owning T value) {}

  public static void main(String[] args) throws Exception {
    Resource x = new Resource();
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

  // It's quite undesirable that this error isn't required.method.not.called.
  // :: error: (required.method.not.called)
  public static <T extends java.io.Closeable> void sneakyDrop5(@Owning T value) {}

  public static void main5(String[] args) throws Exception {
    Resource x = new Resource();
    sneakyDrop5(x);
  }

  // TODO: The error here is a false positive: the Called Methods Checker for some reason fails to
  // recognize that value has had close() called on it. The problem appears to be that
  // the Called Methods Checker's store isn't updated. I suspect the problem has to do with
  // generics/type variables.
  public static <T extends java.io.Closeable> void sneakyDropCorrect(
      // :: error: required.method.not.called
      @Owning @MustCall("close") T value) throws Exception {
    value.close();
  }

  public static void main6(String[] args) throws Exception {
    Resource x = new Resource();
    sneakyDropCorrect(x);
  }
}
