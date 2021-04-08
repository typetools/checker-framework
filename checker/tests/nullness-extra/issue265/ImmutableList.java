import java.util.ArrayList;
import java.util.List;

public abstract class ImmutableList<E> implements List<E> {
  public static <E> List<E> copyOf(Iterable<? extends E> elements) {
    return new ArrayList<E>();
  }
}
