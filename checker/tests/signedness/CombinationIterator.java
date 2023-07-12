import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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
  public List<T> next() {
    return null;
  }
}
