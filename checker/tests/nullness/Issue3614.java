// Test case for https://tinyurl.com/cfissue/3614

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

public class Issue3614 {

  public static @Nullable Boolean m1(@PolyNull Boolean b) {
    return (b == null) ? b : b;
  }

  public static @NonNull Boolean m2(@PolyNull Boolean b) {
    return (b == null) ? Boolean.TRUE : !b;
  }

  public static @PolyNull Boolean m3(@PolyNull Boolean b) {
    return (b == null) ? null : Boolean.TRUE;
  }

  public static @PolyNull Boolean m4(@PolyNull Boolean b) {
    return (b == null) ? null : b;
  }

  public static @PolyNull Boolean m5(@PolyNull Boolean b) {
    return (b == null) ? b : Boolean.TRUE;
  }

  public static @PolyNull Boolean not1(@PolyNull Boolean b) {
    return (b == null) ? null : !b;
  }

  public static @PolyNull Boolean not2(@PolyNull Boolean b) {
    // :: error: (unboxing.of.nullable)
    return (b == null) ? b : !b;
  }

  public static @PolyNull Boolean not3(@PolyNull Boolean b) {
    if (b == null) {
      return null;
    } else {
      return !b;
    }
  }

  public static <@Nullable T> T of1(T a) {
    return a == null ? null : a;
  }

  public static <@Nullable T> T of2(T a) {
    if (a == null) {
      return null;
    } else {
      return a;
    }
  }

  public static @PolyNull Integer plus1(@PolyNull Integer b0, @PolyNull Integer b1) {
    return (b0 == null || b1 == null) ? null : (b0 + b1);
  }

  public static @PolyNull Integer plus2(@PolyNull Integer b0, @PolyNull Integer b1) {
    if (b0 == null || b1 == null) {
      return null;
    } else {
      return b0 + b1;
    }
  }

  public static @PolyNull Integer plus3(@PolyNull Integer a, @PolyNull Integer b) {
    if (a == null) {
      return null;
    }
    if (b == null) {
      return null;
    }
    return a + b;
  }

  public static @PolyNull Integer plus1Err(@PolyNull Integer b0, @PolyNull Integer b1) {
    // :: error: (return.type.incompatible) :: error: (unboxing.of.nullable)
    return (b0 == null) ? null : (b0 + b1);
  }

  public static @PolyNull Integer plus2Err(@PolyNull Integer b0, @PolyNull Integer b1) {
    if (b0 == null) {
      return null;
    } else {
      // :: error: (unboxing.of.nullable)
      return b0 + b1;
    }
  }

  public static @PolyNull Integer plus3Err(@PolyNull Integer a, @PolyNull Integer b) {
    if (a == null) {
      return null;
    }
    // :: error: (unboxing.of.nullable)
    return a + b;
  }

  public static @PolyNull /*("elt")*/ String @PolyNull /*("container")*/ [] typeArray(
      @PolyNull /*("elt")*/ Object @PolyNull /*("container")*/ [] seq) {
    if (seq == null) {
      return null;
    }
    @PolyNull /*("elt")*/ String[] retval = new @PolyNull /*("elt")*/ String[seq.length];
    for (int i = 0; i < seq.length; i++) {
      if (seq[i] == null) {
        retval[i] = null;
      } else {
        retval[i] = seq[i].getClass().toString();
      }
    }
    return retval;
  }
}
