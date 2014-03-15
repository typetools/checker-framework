import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public abstract class ImmutableList<E> implements List<E> {
  public static <E> List<E> copyOf(Iterable<? extends E> elements) {
    return new ArrayList<E>();
  }
}
