import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class CombinationIterator<T> implements Iterator<List<T>> {
  public CombinationIterator(Collection<? extends Collection<T>> collectionsOfCandidates) {
    ArrayList<? extends Collection<T>> listOfCollectionsOfCanditates =
        new ArrayList<>(collectionsOfCandidates);
  }

  @Override
  public boolean hasNext() {
    return false;
  }

  @Override
  @SideEffectsOnly("this")
  public List<T> next() {
    return null;
  }
}
