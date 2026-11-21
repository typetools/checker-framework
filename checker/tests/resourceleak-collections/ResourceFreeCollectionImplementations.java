// Classes that implement collection interfaces but hold no resources need no collection
// ownership annotations.

import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

class ResourceFreeCollectionImplementations {

  static class StringIterator implements Iterator<String> {
    private final String[] data;
    private int i = 0;

    StringIterator(String[] data) {
      this.data = data;
    }

    private boolean inRange() {
      return i < data.length;
    }

    @Override
    public boolean hasNext() {
      return inRange();
    }

    @Override
    public String next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return data[i++];
    }
  }

  static class StringIterable implements Iterable<String> {
    List<String> l = new ArrayList<>();

    private Iterator<String> helper() {
      return l.iterator();
    }

    @Override
    public Iterator<String> iterator() {
      return helper();
    }
  }

  static class StringList extends AbstractList<String> {
    private final List<String> backing = new ArrayList<>();

    @Override
    public String get(int i) {
      return backing.get(i);
    }

    @Override
    public int size() {
      return backing.size();
    }

    @Override
    public String set(int i, String s) {
      return backing.set(i, s);
    }

    @Override
    public void add(int i, String s) {
      backing.add(i, s);
    }
  }

  static class StringEntry implements Map.Entry<String, Integer> {
    @Override
    public String getKey() {
      return "k";
    }

    @Override
    public Integer getValue() {
      return 1;
    }

    @Override
    public Integer setValue(Integer v) {
      return v;
    }
  }

  static class TrimmingMap extends HashMap<String, String> {
    @Override
    public String put(String k, String v) {
      return super.put(k, v.trim());
    }
  }

  static class StringDeque extends ArrayDeque<String> {
    @Override
    public String peek() {
      return super.peek();
    }
  }

  static Iterator<String> countdown(int n) {
    return new Iterator<String>() {
      int k = n;

      @Override
      public boolean hasNext() {
        return k > 0;
      }

      @Override
      public String next() {
        return String.valueOf(k--);
      }
    };
  }
}
