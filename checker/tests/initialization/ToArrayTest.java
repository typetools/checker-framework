import java.util.Collection;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class ToArrayTest {

  public static void isReverse(@NonNull Collection<? extends @Interned Object> seq1) {
    @Interned Object[] seq1_array = seq1.toArray(new @Interned Object[] {});
  }
}
