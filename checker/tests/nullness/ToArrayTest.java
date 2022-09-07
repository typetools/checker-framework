import java.util.Collection;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class ToArrayTest {

  public static void isReverse1(@NonNull Collection<? extends Object> seq1) {
    Object[] seq1_array_TMP2 = seq1.toArray();
    Object[] seq1_array = seq1.toArray(new Object[] {});
  }

  public static void isReverse2(@NonNull Collection<? extends Object> seq1) {
    @NonNull Object @NonNull [] seq1_array_TMP = new Object[] {};
    Object[] seq1_array = seq1.toArray(new Object[] {});
  }

  // No problem here, with explicit upper bound.
  public static void isReverse3(@NonNull Collection<? extends @NonNull Object> seq1) {
    @NonNull Object @NonNull [] seq1_array_TMP = new Object[] {};
    Object[] seq1_array = seq1.toArray(new Object[] {});
  }
}
