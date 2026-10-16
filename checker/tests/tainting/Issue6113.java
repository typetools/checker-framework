import java.util.Collection;
import java.util.Collections;
import java.util.List;

class Issue6113<E> {
  public void bar(List<E> list) {
    Collection<? extends E> c =
        list != null ? list : Collections.emptyList(); // reported error here
  }
}
