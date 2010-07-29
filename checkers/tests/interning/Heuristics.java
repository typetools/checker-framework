import java.util.Comparator;

public class Heuristics {

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
    return false;
  }

  public boolean optimizeEqualsClient(Object a, Object b) {
    // Using == is OK if it's the left-hand side of an || whose right-hand
    // side is a call to equals with the same arguments.
    // Some people like to check reference equality themselves first.
    // This is a premature optimization, but it exists nonetheless.

    // TO DO: Remove this "//::" suppression, because the checker should not issue a warning.
    //:: (not.interned)
    return (a == b || a.equals(b));
  }
}
