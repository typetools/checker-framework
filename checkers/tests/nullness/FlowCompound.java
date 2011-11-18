import checkers.nullness.quals.*;
import java.util.*;

class FlowCompound {

    public boolean equals(@Nullable Object o) {
        return o != null && this.getClass() != o.getClass();
    }

    void test() {

        String s = "foo";

        if (s == null || s.length() > 0) {
            @NonNull String test = s;
        }

        String tmp;
        @NonNull String notNull;
        tmp = "hello";
        notNull = tmp;
        notNull = tmp = "hello";

    }

  public static boolean equal(@Nullable Object a, @Nullable Object b) {
      assert b != null : "suppress nullness";
    return a == b || (a != null && a.equals(b));
  }


  public static void testCompoundAssignmentWithString() {
      String s = "m";
      s += "n";
      s.toString();
  }

  public static void testCompoundAssignmentWithChar() {
      String s = "m";
      s += 'n';
      s.toString();
  }

  public static void testCompoundAssignWithNull() {
      String s = "m";
      s += null;
      s.toString();
  }

  public static void testPrimitiveArray() {
      int[] a = {0};
      a[0] += 2;
      System.out.println(a[0]);
  }

  public static void testPrimitive() {
      Integer i = 1;
      i -= 2;
  }
}
