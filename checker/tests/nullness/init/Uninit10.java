import org.checkerframework.checker.nullness.qual.*;

public class Uninit10 {

  @NonNull String[] strings;

  // :: error: (initialization.fields.uninitialized)
  Uninit10() {}

  public class Inner {

    @NonNull String[] stringsInner;

    // :: error: (initialization.fields.uninitialized)
    Inner() {}
  }
}
