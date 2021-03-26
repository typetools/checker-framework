import java.util.Comparator;
import org.checkerframework.checker.interning.qual.CompareToMethod;
import org.checkerframework.checker.interning.qual.EqualsMethod;

public class HeuristicsTest implements Comparable<HeuristicsTest> {

  public static final class MyComparator implements Comparator<String> {
    // Using == is OK if it's the first statement in the compare method,
    // it's comparing the arguments, and the return value is 0.
    public int compare(String s1, String s2) {
      if (s1 == s2) {
        return 0;
      }
      return String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
    }
  }

  @Override
  @org.checkerframework.dataflow.qual.Pure
  public boolean equals(Object o) {
    // Using == is OK if it's the first statement in the equals method
    // and it compares "this" against the argument.
    if (this == o) {
      return true;
    }
    // Not the first statement in the method.
    // :: error: (not.interned)
    if (o == this) {
      return true;
    }
    return false;
  }

  @EqualsMethod
  @org.checkerframework.dataflow.qual.Pure
  public boolean equals2(Object o) {
    // Using == is OK if it's the first statement in the equals method
    // and it compares "this" against the argument.
    if (this == o) {
      return true;
    }
    // Not the first statement in the method.
    // :: error: (not.interned)
    if (o == this) {
      return true;
    }
    return false;
  }

  @org.checkerframework.dataflow.qual.Pure
  public boolean equals3(Object o) {
    // Not equals() or annotated as @EqualsMethod.
    // :: error: (not.interned)
    if (this == o) {
      return true;
    }
    // Not the first statement in the method.
    // :: error: (not.interned)
    if (o == this) {
      return true;
    }
    return false;
  }

  @EqualsMethod
  @org.checkerframework.dataflow.qual.Pure
  public static boolean equals4(Object thisOne, Object o) {
    // Using == is OK if it's the first statement in the equals method
    // and it compares "this" against the argument.
    if (thisOne == o) {
      return true;
    }
    // Not the first statement in the method.
    // :: error: (not.interned)
    if (o == thisOne) {
      return true;
    }
    return false;
  }

  @org.checkerframework.dataflow.qual.Pure
  public static boolean equals5(Object thisOne, Object o) {
    // Not equals() or annotated as @EqualsMethod.
    // :: error: (not.interned)
    if (thisOne == o) {
      return true;
    }
    // Not the first statement in the method.
    // :: error: (not.interned)
    if (o == thisOne) {
      return true;
    }
    return false;
  }

  @EqualsMethod
  // :: error: (invalid.method.annotation)
  public boolean equals6() {
    return true;
  }

  @EqualsMethod
  // :: error: (invalid.method.annotation)
  public boolean equals7(int a, int b, int c) {
    return true;
  }

  @Override
  @org.checkerframework.dataflow.qual.Pure
  public int compareTo(HeuristicsTest o) {
    // Using == is OK if it's the first statement in the equals method
    // and it compares "this" against the argument.

    if (o == this) {
      return 0;
    }
    // Not the first statement in the method.
    // :: error: (not.interned)
    if (this == o) {
      return 0;
    }
    return 0;
  }

  @CompareToMethod
  @org.checkerframework.dataflow.qual.Pure
  public int compareTo2(HeuristicsTest o) {
    // Using == is OK if it's the first statement in the equals method
    // and it compares "this" against the argument.

    if (o == this) {
      return 0;
    }
    // Not the first statement in the method.
    // :: error: (not.interned)
    if (this == o) {
      return 0;
    }
    return 0;
  }

  @org.checkerframework.dataflow.qual.Pure
  public int compareTo3(HeuristicsTest o) {
    // Not compareTo or annotated as @CompareToMethod
    // :: error: (not.interned)
    if (o == this) {
      return 0;
    }
    // Not the first statement in the method.
    // :: error: (not.interned)
    if (this == o) {
      return 0;
    }
    return 0;
  }

  @CompareToMethod
  @org.checkerframework.dataflow.qual.Pure
  public static int compareTo4(HeuristicsTest thisOne, HeuristicsTest o) {
    // Using == is OK if it's the first statement in the equals method
    // and it compares "this" against the argument.

    if (o == thisOne) {
      return 0;
    }
    // Not the first statement in the method.
    // :: error: (not.interned)
    if (thisOne == o) {
      return 0;
    }
    return 0;
  }

  @org.checkerframework.dataflow.qual.Pure
  public static int compareTo5(HeuristicsTest thisOne, HeuristicsTest o) {
    // Not compareTo or annotated as @CompareToMethod
    // :: error: (not.interned)
    if (o == thisOne) {
      return 0;
    }
    // Not the first statement in the method.
    // :: error: (not.interned)
    if (thisOne == o) {
      return 0;
    }
    return 0;
  }

  @EqualsMethod
  // :: error: (invalid.method.annotation)
  public boolean compareTo6() {
    return true;
  }

  @EqualsMethod
  // :: error: (invalid.method.annotation)
  public boolean compareTo7(int a, int b, int c) {
    return true;
  }

  public boolean optimizeEqualsClient(Object a, Object b, Object[] arr) {
    // Using == is OK if it's the left-hand side of an || whose right-hand
    // side is a call to equals with the same arguments.
    if (a == b || a.equals(b)) {
      System.out.println("one");
    }
    if (a == b || b.equals(a)) {
      System.out.println("two");
    }

    boolean c = (a == b || a.equals(b));
    c = (a == b || b.equals(a));

    boolean d = (a == b) || (a != null ? a.equals(b) : false);

    boolean e = (a == b || (a != null && a.equals(b)));

    boolean f = (arr[0] == a || arr[0].equals(a));

    return (a == b || a.equals(b));
  }

  public <T extends Comparable<T>> boolean optimizeCompareToClient(T a, T b) {
    // Using == is OK if it's the left-hand side of an || whose right-hand
    // side is a call to compareTo with the same arguments.
    if (a == b || a.compareTo(b) == 0) {
      System.out.println("one");
    }
    if (a == b || b.compareTo(a) == 0) {
      System.out.println("two");
    }

    boolean c = (a == b || a.compareTo(b) == 0);
    c = (a == b || a.compareTo(b) == 0);

    return (a == b || a.compareTo(b) == 0);
  }
}
