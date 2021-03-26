import org.checkerframework.checker.nullness.qual.NonNull;

public class SuppressWarningsPartialKeys {

  @SuppressWarnings("return.type.incompatible")
  @NonNull Object suppressed2() {
    return null;
  }

  @SuppressWarnings("return.type")
  @NonNull Object suppressed3() {
    return null;
  }

  @SuppressWarnings("type.incompatible")
  @NonNull Object suppressed4() {
    return null;
  }

  @SuppressWarnings("type")
  @NonNull Object suppressed5() {
    return null;
  }

  @SuppressWarnings("nullness:return.type.incompatible")
  @NonNull Object suppressedn2() {
    return null;
  }

  @SuppressWarnings("nullness:return.type")
  @NonNull Object suppressedn3() {
    return null;
  }

  @SuppressWarnings("nullness:type.incompatible")
  @NonNull Object suppressedn4() {
    return null;
  }

  @SuppressWarnings("nullness:type")
  @NonNull Object suppressedn5() {
    return null;
  }

  @SuppressWarnings("i")
  @NonNull Object err1() {
    // :: error: (return.type.incompatible)
    return null;
  }

  @SuppressWarnings("eturn.type")
  @NonNull Object err2() {
    // :: error: (return.type.incompatible)
    return null;
  }

  @SuppressWarnings("typ")
  @NonNull Object err3() {
    // :: error: (return.type.incompatible)
    return null;
  }

  @SuppressWarnings("ype.incompatible")
  @NonNull Object err4() {
    // :: error: (return.type.incompatible)
    return null;
  }

  @SuppressWarnings("return.type.")
  @NonNull Object err5() {
    // :: error: (return.type.incompatible)
    return null;
  }

  @SuppressWarnings(".type.incompatible")
  @NonNull Object err6() {
    // :: error: (return.type.incompatible)
    return null;
  }

  @SuppressWarnings("nullness:i")
  @NonNull Object errn1() {
    // :: error: (return.type.incompatible)
    return null;
  }

  @SuppressWarnings("nullness:eturn.type")
  @NonNull Object errn2() {
    // :: error: (return.type.incompatible)
    return null;
  }

  @SuppressWarnings("nullness:typ")
  @NonNull Object errn3() {
    // :: error: (return.type.incompatible)
    return null;
  }

  @SuppressWarnings("nullness:ype.incompatible")
  @NonNull Object errn4() {
    // :: error: (return.type.incompatible)
    return null;
  }

  @SuppressWarnings("nullness:return.type.")
  @NonNull Object errn5() {
    // :: error: (return.type.incompatible)
    return null;
  }

  @SuppressWarnings("nullness:.type.incompatible")
  @NonNull Object errn6() {
    // :: error: (return.type.incompatible)
    return null;
  }
}
