// Test that checks for a crash in WPI that was caused by
// an atypical expression in the first initializer of a for loop
// (in this case, the "index++" in LightWeightGSet#nextNonemptyEntry).

import java.util.*;

@SuppressWarnings("allcheckers") // Only check for crashes
class LightWeightCache<K, E extends K> extends LightWeightGSet<K, E> {
  @Override
  public Iterator<E> iterator() {
    final Iterator<E> iter = super.iterator();
    return new Iterator<E>() {
      @Override
      public boolean hasNext() {
        return iter.hasNext();
      }

      @Override
      public E next() {
        return iter.next();
      }

      @Override
      public void remove() {}
    };
  }
}

@SuppressWarnings("allcheckers") // Only check for crashes
class LightWeightGSet<K, E extends K> implements GSet<K, E> {

  interface LinkedElement {}

  protected LinkedElement[] entries;

  @Override
  public Iterator<E> iterator() {
    return new SetIterator();
  }

  private class SetIterator implements Iterator<E> {
    private int index = -1;
    private LinkedElement next = nextNonemptyEntry();

    private LinkedElement nextNonemptyEntry() {
      for (index++; index < entries.length && entries[index] == null; index++)
        ;
      return index < entries.length ? entries[index] : null;
    }

    @Override
    public E next() {
      return null;
    }

    @Override
    public boolean hasNext() {
      return false;
    }
  }
}

interface GSet<K, E extends K> extends Iterable<E> {}
