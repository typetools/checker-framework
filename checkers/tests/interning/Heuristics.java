import java.util.Comparator;

public class Heuristics implements Comparable<Heuristics> {

   public static final class MyComparator implements Comparator<String> {
     // Using == is OK if it's the first statement in the compare method,
     // it's comparing the arguments, and the return value is 0.
     public int compare(String s1, String s2) {
       if (s1 == s2)
         return 0;
       return String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
     }
   }

  @Override
  public boolean equals(Object o) {
    // Using == is OK if it's the first statement in the equals method
    // and it compares "this" against the argument.
    if (this == o) return true;
    if (o == this) return true;
    return false;
  }

  @Override
  public int compareTo(Heuristics o) {
    // Using == is OK if it's the first statement in the equals method
    // and it compares "this" against the argument.

    if (o == this) return 0;
    if (this == o) return 0;
    return 0;
  }

  public boolean optimizeEqualsClient(Object a, Object b) {
    // Using == is OK if it's the left-hand side of an || whose right-hand
    // side is a call to equals with the same arguments.

    // TO DO: Remove this "//::" suppression, because the checker should not issue a warning.
    //:: (not.interned)
    return (a == b || a.equals(b));
  }

  public <T extends Comparable<T>> boolean optimizeCompareToClient(T a, T b) {
    // Using == is OK if it's the left-hand side of an || whose right-hand
    // side is a call to compareTo with the same arguments.

    // TO DO: Remove this "//::" suppression, because the checker should not issue a warning.
    //:: (not.interned)
    return (a == b || a.compareTo(b) == 0);
  }
}
