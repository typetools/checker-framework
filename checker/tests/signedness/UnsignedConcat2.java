import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class UnsignedConcat2<T extends @NonNull Object> {

  protected @Nullable T value;

  protected Set<T> set;

  @Override
  @SuppressWarnings("signedness:unsigned.concat") // don't restrict instantiation just for toString
  public String toString() {
    switch (set.size()) {
      case 0:
        return "[]";
      case 1:
        return "[" + value + "]";
      default:
        return set.toString();
    }
  }
}
