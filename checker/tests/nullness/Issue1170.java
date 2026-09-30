// Test case for https://github.com/typetools/checker-framework/issues/1170

import org.checkerframework.checker.nullness.qual.PolyNull;

public class Issue1170 {

  public static @PolyNull String intern1(@PolyNull String a) {
    if (a == null) {
      return null;
    }
    return a.intern();
  }

  public static @PolyNull String intern2(@PolyNull String a) {
    return (a == null) ? null : a.intern();
  }

  @PolyNull String quoted1(@PolyNull String arg) {
    return arg == null ? null : "\"" + arg + "\"";
  }

  @PolyNull String quoted2(@PolyNull String arg) {
    return arg == null ? arg : "\"" + arg + "\"";
  }

  @PolyNull String quoted3(@PolyNull String arg) {
    return arg != null ? "\"" + arg + "\"" : null;
  }

  @PolyNull String quoted4(@PolyNull String arg) {
    return arg != null ? "\"" + arg + "\"" : arg;
  }
}
