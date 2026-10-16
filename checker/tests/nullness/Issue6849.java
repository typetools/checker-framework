// Test case for https://github.com/typetools/checker-framework/issues/6849

import java.util.LinkedList;
import java.util.List;
import java.util.function.IntSupplier;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue6849 {

  public static <T> T m(List<T> lst) {
    return lst.get(0);
  }

  public static void main(String[] args) {
    List<@Nullable Integer> lst = new LinkedList<>();
    lst.add(null);
    // :: error: (unboxing.of.nullable)
    int y = ((true) ? Issue6849.<@Nullable Integer>m(lst) : 10);
  }

  static void takeInt(int i) {}

  static void takeObject(@Nullable Object o) {}

  static <U> void takeGeneric(U u) {}

  static void assign(boolean b, List<@Nullable Integer> lst) {
    // :: error: (unboxing.of.nullable)
    int y1 = b ? Issue6849.<@Nullable Integer>m(lst) : 10;
    // :: error: (unboxing.of.nullable)
    long y2 = (b ? 10 : Issue6849.<@Nullable Integer>m(lst));
    int y3;
    // :: error: (unboxing.of.nullable)
    y3 = (b ? Issue6849.<@Nullable Integer>m(lst) : 10);
    // :: error: (unboxing.of.nullable)
    int y4 = b ? (b ? Issue6849.<@Nullable Integer>m(lst) : 10) : 20;
    // :: error: (unboxing.of.nullable)
    int y5 = (b ? Issue6849.<@Nullable Integer>m(lst) : 10) + 1;
  }

  static int returnConditional(boolean b, List<@Nullable Integer> lst) {
    // :: error: (unboxing.of.nullable)
    return b ? Issue6849.<@Nullable Integer>m(lst) : 10;
  }

  static void argument(boolean b, List<@Nullable Integer> lst) {
    // :: error: (unboxing.of.nullable)
    takeInt(b ? Issue6849.<@Nullable Integer>m(lst) : 10);
  }

  static int switchExpression(int k, boolean b, List<@Nullable Integer> lst) {
    return switch (k) {
      // :: error: (unboxing.of.nullable)
      case 1 -> (b ? Issue6849.<@Nullable Integer>m(lst) : 10);
      default -> 10;
    };
  }

  static IntSupplier lambda(boolean b, List<@Nullable Integer> lst) {
    // :: error: (unboxing.of.nullable)
    return () -> b ? Issue6849.<@Nullable Integer>m(lst) : 10;
  }

  static void standalone(boolean b, @Nullable Integer n, @Nullable Integer n2) {
    // :: error: (unboxing.of.nullable)
    takeInt(b ? n : 10);
    // :: error: (unboxing.of.nullable)
    takeObject(b ? n2 : 10);
  }

  // The operands are boxed, not unboxed, so no unboxing errors are issued.
  static void noUnboxing(boolean b, List<@Nullable Integer> lst) {
    Object o = (b ? Issue6849.<@Nullable Integer>m(lst) : 10);
    @Nullable Integer i = b ? Issue6849.<@Nullable Integer>m(lst) : 10;
    takeObject(b ? Issue6849.<@Nullable Integer>m(lst) : 10);
    takeGeneric(b ? Issue6849.<@Nullable Integer>m(lst) : 10);
  }

  static void nonNull(boolean b, List<Integer> lst) {
    int y = (b ? Issue6849.<Integer>m(lst) : 10);
    takeInt(b ? Issue6849.<Integer>m(lst) : 10);
  }
}
