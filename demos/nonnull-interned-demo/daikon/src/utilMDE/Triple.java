package utilMDE;

/**
 * Simple triple class.
 **/
public class Triple<T1,T2,T3> {
  public T1 a;
  public T2 b;
  public T3 c;

  public Triple(T1 a, T2 b, T3 c) {
    this.a = a;
    this.b = b;
    this.c = c;
  }

  public String toString() {
    return "<" + String.valueOf(a)
      + "," + String.valueOf(b)
      + "," + String.valueOf(c)
      + ">";
  }

  public boolean equals(Object obj) {
    if (obj instanceof Triple) {
      Triple other = (Triple) obj;
      boolean aEquals = ((this.a == null && other.a == null) ||
                         (this.a.equals(other.a)));
      boolean bEquals = ((this.b == null && other.b == null) ||
                         (this.b.equals(other.b)));
      boolean cEquals = ((this.c == null && other.c == null) ||
                         (this.c.equals(other.c)));
      return aEquals && bEquals && cEquals;
    } else {
      return false;
    }
  }

  public int hashCode() {
    return (((a == null) ? 0 : a.hashCode())
            + ((b == null) ? 0 : b.hashCode())
            + ((c == null) ? 0 : c.hashCode()));
  }

}
