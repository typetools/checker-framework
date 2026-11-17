import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// Ignored positions whose instantiation is a raw type with a recursive bound.  The type argument
// of the raw type R is a wildcard that is reachable from its own bound, so a walk of corresponding
// positions must not follow wildcard bounds forever.  B = R or B = Enum comes from a use with an
// explicit annotation, so it ignores annotations.
@SuppressWarnings("rawtypes")
public class RawRecursiveBoundOfIgnoredPosition {
  static class Box<T> {}

  static class R<T extends R> {}

  static <B extends @Nullable Object> B[] arrOf(Box<@NonNull B> b, B other) {
    throw new RuntimeException();
  }

  static <B extends @Nullable Object> List<? extends B> listOf(Box<@NonNull B> b, B other) {
    throw new RuntimeException();
  }

  static <B extends @Nullable Object> B one(Box<@NonNull B> b, B other) {
    throw new RuntimeException();
  }

  static <S extends @Nullable Object> S pick2(S a, S b) {
    return a;
  }

  static void test(
      Box<@NonNull R> boxR,
      Box<@NonNull Enum> boxE,
      @Nullable R nullableR,
      @Nullable Enum nullableE,
      @Nullable R[] nullableRArr,
      @Nullable Enum[] nullableEArr,
      List<? extends @Nullable R> nullableRList) {
    // :: error: (dereference.of.nullable)
    pick2(arrOf(boxR, nullableR), nullableRArr)[0].toString();
    // :: error: (dereference.of.nullable)
    pick2(arrOf(boxE, nullableE), nullableEArr)[0].toString();
    // :: error: (dereference.of.nullable)
    pick2(arrOf(boxR, nullableR), arrOf(boxR, nullableR))[0].toString();
    // :: error: (dereference.of.nullable)
    pick2(listOf(boxR, nullableR), nullableRList).get(0).toString();
    // :: error: (dereference.of.nullable)
    pick2(one(boxR, nullableR), nullableR).toString();
    // :: error: (dereference.of.nullable)
    pick2(one(boxR, nullableR), one(boxR, nullableR)).toString();
    // :: error: (dereference.of.nullable)
    pick2(one(boxE, nullableE), one(boxE, nullableE)).toString();
  }
}
