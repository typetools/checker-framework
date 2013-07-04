// Test case for issue #234.
// Some of the lines in this file should issue warnings, but not all should.

import checkers.nullness.quals.*;

import java.util.List;
import java.util.Iterator;

public class GuavaErrorSimple {

  public static void client1(List<String> strings) {
    join1(strings.iterator());
    join2(strings.iterator());
    join3(strings.iterator());
    join4(strings.iterator());
  }

  public static void client2(Iterator<String> itor) {
    join1(itor);
    join2(itor);
    join3(itor);
    join4(itor);
  }

  public static void client3(Iterator<String> itor) {
    Iterator<?> parts1 = itor;
    Iterator<? extends Object> parts2 = itor;
    Iterator<? extends @Nullable Object> parts3 = itor;
    Iterator<? extends @NonNull Object> parts4 = itor;
  }

  static void join1(Iterator<?> parts) {
  }

  static void join2(Iterator<? extends Object> parts) {
  }

  static void join3(Iterator<? extends @Nullable Object> parts) {
  }

  static void join4(Iterator<? extends @NonNull Object> parts) {
  }

}
