package open.caughtcrash;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("all")
public class Issue6762 {

  void method(Optional<String> opt, Stream<Integer> intStream) {
    opt.map(
        rids ->
            intStream.collect(
                Collectors.collectingAndThen(Collectors.toSet(), i -> new IntArraySet(i))));
  }

  static class IntArraySet extends IntSet {

    IntArraySet(Set<Integer> i) {}
  }

  static class IntSet implements Set<Integer> {

    @Override
    public int size() {
      return 0;
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public boolean contains(Object o) {
      return false;
    }

    @Override
    public Iterator<Integer> iterator() {
      return null;
    }

    @Override
    public Object[] toArray() {
      return new Object[0];
    }

    @Override
    public <T> T[] toArray(T[] a) {
      return null;
    }

    @Override
    public boolean add(Integer integer) {
      return false;
    }

    @Override
    public boolean remove(Object o) {
      return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      return false;
    }

    @Override
    public boolean addAll(Collection<? extends Integer> c) {
      return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      return false;
    }

    @Override
    public void clear() {}
  }
}
