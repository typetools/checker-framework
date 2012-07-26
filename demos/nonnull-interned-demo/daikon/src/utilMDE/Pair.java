package utilMDE;

/**
 * Simple pair class.  Moved from OrderedPairIterator to its own
 * class, so it can be used in more places.
 **/
public class Pair<T1,T2> {
  public T1 a;
  public T2 b;

  public Pair(T1 a, T2 b) {
    this.a = a;
    this.b = b;
  }

  public String toString() {
    return "<" + String.valueOf(a) + "," + String.valueOf(b) + ">";
  }

  public boolean equals(Object obj) {
    if (obj instanceof Pair) {
      Pair other = (Pair) obj;
      boolean aEquals = ((this.a == null && other.a == null) ||
                         (this.a.equals(other.a)));
      boolean bEquals = ((this.b == null && other.b == null) ||
                         (this.b.equals(other.b)));
      return aEquals && bEquals;
    } else {
      return false;
    }
  }

  public int hashCode() {
    return (((a == null) ? 0 : a.hashCode()) +
            ((b == null) ? 0 : b.hashCode()));
  }

}
