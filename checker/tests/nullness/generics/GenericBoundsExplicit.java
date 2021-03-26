package nullness.generics;

import org.checkerframework.checker.nullness.qual.*;

public class GenericBoundsExplicit<@NonNull T extends @Nullable Object> {

  @SuppressWarnings("initialization.field.uninitialized")
  T t;

  public void method() {
    // :: error: (dereference.of.nullable)
    String str = t.toString();
  }

  public static void doSomething() {
    final GenericBoundsExplicit<@Nullable String> b = new GenericBoundsExplicit<@Nullable String>();
    b.method();
  }
}

class GenericBoundsExplicit2<@NonNull TT extends @Nullable Object> {
  @Nullable TT tt1;
  // :: error: (initialization.field.uninitialized)
  @NonNull TT tt2;
  // :: error: (initialization.field.uninitialized)
  TT tt3;

  public void context() {

    // :: error: (dereference.of.nullable)
    tt1.toString();
    tt2.toString();

    // :: error: (dereference.of.nullable)
    tt3.toString();
  }
}

@SuppressWarnings("initialization.field.uninitialized")
class GenericBoundsExplicit3<@NonNull TTT extends @NonNull Object> {
  @Nullable TTT ttt1;
  @NonNull TTT ttt2;
  TTT ttt3;

  public void context() {
    // :: error: (dereference.of.nullable)
    ttt1.toString();
    ttt2.toString();
    ttt3.toString();
  }
}
