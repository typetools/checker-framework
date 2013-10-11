// @skip-test Crashes the Checker Framework, but skipped to avoid breaking the build

import java.util.*;

public class SortingCollection<T> {

    class MergingIterator {
        private final PollableTreeSet<String> queue = null;

        public boolean hasNext() {
            return !queue.isEmpty();
        }

    }

    static class PollableTreeSet<T> extends TreeSet<T> {
    }

}
