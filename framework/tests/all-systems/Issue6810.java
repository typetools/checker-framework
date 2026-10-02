import java.util.Set;
import org.checkerframework.checker.modifiability.qual.Modifiable;

public class Issue6810 {
  static class Box<T extends Box<T>> {}

  abstract static class BoxSet<T extends Box<T>> implements Set<T> {}

  static <E extends Box<E>> void intersect2(
      @Modifiable BoxSet<? extends E> intersect, Set<E> set2) {
    intersect.retainAll(set2);
  }
}
