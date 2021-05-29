import java.util.Comparator;
import org.checkerframework.checker.interning.qual.Interned;

public class ComplexComparison {

  void testInterned() {

    @Interned String a = "foo";
    @Interned String b = "bar";

    if (a != null && b != null && a == b) {
      System.out.println("yes");
    } else {
      System.out.println("no");
    }
  }

  void testInternedDueToFlow() {

    String c = "foo";
    String d = "bar";

    if (c != null && d != null && c == d) {
      System.out.println("yes");
    } else {
      System.out.println("no");
    }
  }

  void testNotInterned() {

    String e = new String("foo");
    String f = new String("bar");

    // :: error: (not.interned)
    if (e != null && f != null && e == f) {
      System.out.println("yes");
    } else {
      System.out.println("no");
    }
  }

  /* @ pure */ public class DoubleArrayComparatorLexical implements Comparator<double[]> {

    /**
     * Lexically compares o1 and o2 as double arrays.
     *
     * @return positive if o1 > 02, 0 if 01 == 02, negative if 01 < 02
     */
    public int compare(double[] a1, double[] a2) {
      // Heuristic: permit "arg1 == arg2" in a test in the first statement
      // of a "Comparator.compare" method, if the body just returns 0.
      if (a1 == a2) {
        return 0;
      }
      int len = Math.min(a1.length, a2.length);
      for (int i = 0; i < len; i++) {
        if (a1[i] != a2[i]) {
          return ((a1[i] > a2[i]) ? 1 : -1);
        }
      }
      return a1.length - a2.length;
    }
  }

  class C {
    @Override
    @org.checkerframework.dataflow.qual.Pure
    public boolean equals(Object other) {
      // Heuristic: permit "this == arg1" in a test in the first statement
      // of a "Comparator.compare" method, if the body just returns true.
      if (this == other) {
        return true;
      }
      return super.equals(other);
    }
  }

  // // TODO
  // class D {
  //     @Override
  //     public boolean equals(Object other) {
  //         // Don't suppress warnings at "this == arg1" if arg1 has been reassigned
  //         other = new Object();
  //
  //         if (this == other) {
  //             return true;
  //         }
  //         return super.equals(other);
  //     }
  // }

}
