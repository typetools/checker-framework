import java.util.Set;

public class Issue6810 {
  static class Box<T extends Box<T>> {}

  abstract static class BoxSet<T extends Box<T>> implements Set<T> {}

  static <E extends Box<E>> void intersect2(BoxSet<? extends E> intersect, Set<E> set2) {
    intersect.retainAll(set2);
  }
}
