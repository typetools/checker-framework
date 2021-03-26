import java.util.TreeSet;

// @skip-test Crashes the Checker Framework, but skipped to avoid breaking the build
//
// It looks like we are relying on name equality at some point when resolving
// a type parameter.  If you replace T by E, changing the code to:
//    static class PollableTreeSet<E> extends TreeSet<E> {
//    }
// then the assertion failure goes away.  Evidently this is because
// the annotated TreeSet.java file uses the type variable E.

public class SortingCollection<T> {

  class MergingIterator {
    private final PollableTreeSet<String> queue = null;

    public boolean hasNext() {
      return !queue.isEmpty();
    }
  }

  static class PollableTreeSet<T> extends TreeSet<T> {}
}
