import org.checkerframework.checker.nullness.qual.NonNull;

public class SuppressWarningsPartialKeys {

  @SuppressWarnings("return")
  @NonNull Object suppressed2() {
    return null;
  }

  @SuppressWarnings("return")
  @NonNull Object suppressed3() {
    return null;
  }

  @SuppressWarnings("type")
  @NonNull Object suppressed5() {
    // :: error: (return)
    return null;
  }

  @SuppressWarnings("nullness:return")
  @NonNull Object suppressedn2() {
    return null;
  }

  @SuppressWarnings("i")
  @NonNull Object err1() {
    // :: error: (return)
    return null;
  }

  @SuppressWarnings("")
  @NonNull Object err6() {
    // :: error: (return)
    return null;
  }

  @SuppressWarnings("nullness:i")
  @NonNull Object errn1() {
    // :: error: (return)
    return null;
  }

  @SuppressWarnings("nullness:eturn.type")
  @NonNull Object errn2() {
    // :: error: (return)
    return null;
  }

  @SuppressWarnings("nullness:typ")
  @NonNull Object errn3() {
    // :: error: (return)
    return null;
  }

  @SuppressWarnings("nullness:ype.incompatible")
  @NonNull Object errn4() {
    // :: error: (return)
    return null;
  }

  @SuppressWarnings("nullness:return.type.")
  @NonNull Object errn5() {
    // :: error: (return)
    return null;
  }

  @SuppressWarnings("nullness:")
  @NonNull Object errn6() {
    // :: error: (return)
    return null;
  }
}
