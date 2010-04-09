package daikon.dcomp;

import static java.lang.System.out;
import java.io.*;
import java.util.*;

class Test {

  A at;
  static int i;
  static int j;
  static A sa1 = new A ("sa1");
  static A sa2 = new A ("sa2");
  static boolean verbose = false;
  // A[] at_arr;
  // double[] d_arr;

  static class A {
    String id;
    int x;
    int y;

    public A(String id) {
      this.id = id;
      x = 1;
      y = 2;
    }
    public void add() {
      x = x + y;
      if (false)
        throw new RuntimeException ("exception in add");
    }
    public void add(int val) {
      x += val;
    }
    public void tta () {
      add (y);
    }
    public String toString() { return ("A " + id); }
  }

  public static class C {

    String cid;
    long long1;

    C (String id) {
      cid = id;
    }

    public void set_long (long l1) {
      long1 = l1;
    }

    public String toString() {
      return cid;
    }

  }

  public static class B {

    A[] a1a;
    A[] a2a;
    A a1;
    int ii = 2;
    int jj = 1;

    B() {
      a1a = new A[] {new A("a1a-0"), new A("a1a-1"), new A("a1a-2")};
      a2a = new A[] {new A("a2a-0"), new A("a2a-1"), new A("a2a-2"),
                     new A("A2a-3")};
    }

    void ecomp() {

      if (a1a[ii] == a2a[jj]) {
        if (verbose)
          System.out.println ("a1a[2] == a2a[1]");
      } else {
        if (verbose)
          System.out.println ("a1a[2] != a2a[1]");
      }
    }

    void p (A aval) {
      a1a[2].add();
    }

    void comp() {
      if (a1a == a2a) {
        if (verbose)
          System.out.println ("a1a == a2a");
      } else {
        if (verbose)
          System.out.println ("a1a != a2a");
      }

    }
  }


  public static class D {
    int i;
    int j;

    int alen = 10;
    int[] a = new int[alen];
    int b = 42;
    int c = 18;
    int d = 8;

    D() {
      int x = 0;
      for (int ii = 0; ii < a.length; ii++) {
        a[ii] = x;
        x += 10;
      }
    }

    public void compare() {
      if (c > a.length) {
        if (verbose) {
          System.out.println("c > a.length");
        }
      }

      for (i = 0; i < d; i++) {
        if (a[i] > b) {
          if (verbose) {
            System.out.println("a[" + i + "] > b");
          }
        }
      }
    }
  }


  public static class E {
    int i;
    int j;

    int ilen = 10;
    int jlen = 3;
    int[][] a = new int[ilen][jlen];
    int b = 42;
    int ci = 18;
    int cj = 10;
    int di = 8;
    int dj = 2;

    E() {
      int x = 0;
      for (int ii = 0; ii < a.length; ii++) {
        for (int jj = 0; jj < a[ii].length; jj++) {
          a[ii][jj] = x;
          x += 10;
        }
      }
    }

    public void compare() {
      if (ci > a.length) {
        if (verbose) {
          System.out.println("ci > a.length");
        }
      }

      if (cj > a[0].length) {
        if (verbose) {
          System.out.println("cj > a[].length");
        }
      }

      for (i = 0; i < di; i++) {
        for (j = 0; j < dj; j++) {
          if (a[i][j] > b) {
            if (verbose) {
              System.out.println("a[" + i + "][" + j + "] > b");
            }
          }
        }
      }
    }
  }


  // Tests the equals() method
  public static class F {
    Obj obj1;
    Obj obj2;
    ObjSub os1;
    ObjSub os2;
    Integer int1;
    Integer int2;

    int a1 = 4;
    int b1 = 3;
    int c1 = 6;
    int a2 = 4;
    int b2 = 3;
    int c2 = 6;

    F() {
      obj1 = new Obj(a1, b1);
      obj2 = new Obj(a2, b2);
      os1 = new ObjSub(a1, b1, c1);
      os2 = new ObjSub(a2, b2, c2);
      int1 = new Integer(42);
      int2 = new Integer(42);
    }

    // Should make obj1 and obj2 comparable
    public void compare() {
      // Uses the equals() method of a non-JDK class
      if (obj2.equals(obj1)) {
        if (verbose) {
          System.out.println("obj2.equals(obj1)");
        }
      }

      // Uses an equals() method that calls super.equals()
      if (os1.equals(os2)) {
        if (verbose) {
          System.out.println("os1.equals(os2)");
        }
      }

      // Uses the equals() method of a JDK class
      if (int1.equals(int2)) {
        if (verbose) {
          System.out.println("int1.equals(int2)");
        }
      }
    }

    // Should make obj1 and obj2 comparable
    public void compare2() {
      if (((Object)obj2).equals((Object)obj1)) {
        if (verbose) {
          System.out.println("((Object)obj2).equals((Object)obj1)");
        }
      }
    }

    // Should NOT change comparability
    public void compare3() {
      obj1.hashCode();
      obj2.hashCode();
      int1.hashCode();
      int2.hashCode();
    }
  }


  // Tests the clone() method
  public static class G {
    static class Uncloneable {
      protected Object clone() throws CloneNotSupportedException {
        //        return super.clone();
        throw new CloneNotSupportedException();
      }
    };

    Obj obj1;
    Obj obj2;
    Uncloneable u1;
    Uncloneable u2;

    G() {
      obj1 = new Obj(2, 9);
      u1 = new Uncloneable();
    }

    public void compare() {
      try {
        obj2 = (Obj)(obj1.clone());
        assert obj1.x == obj2.x : "Corresponding fields should be equal";
        assert obj1.y == obj2.y : "Corresponding fields should be equal";
      } catch (CloneNotSupportedException e) {
        assert false : "Caught unexpected CloneNotSupportedException";
      }

      // Ensure the clone() method still works when not overridden
      try {
        u2 = (Uncloneable)(u1.clone());
      } catch (CloneNotSupportedException e) {
        return;
      }

      assert false : "Expected CloneNotSupportedException wasn't thrown";
    }
  }




  public static class Arr {

    int[] big_arr = new int[90000];
    int val = 3;

    public Arr() {
      big_arr[70] = val;
    }

    public void tryit (int val1) {
      big_arr[71] = val1;
    }
  }


  public static class Obj implements Cloneable {
    public int x;
    public int y;

    public Obj(int x, int y) {
      this.x = x;
      this.y = y;
    }

    protected Object clone() throws CloneNotSupportedException {
      return super.clone();
    }

    public boolean equals(Object obj) {
      return (obj instanceof Obj)
        && this.x == ((Obj)obj).x
        && this.y == ((Obj)obj).y;
    }

    public int hashCode() {
      return this.x + this.y;
    }

    public String toString() {
      return String.valueOf(this.x) + String.valueOf(this.y);
    }
  }


  public static class ObjSub extends Obj {
    public int z;

    public ObjSub(int x, int y, int z) {
      super(x, y);
      this.z = z;
    }

    // Overrides Obj.equals
    public boolean equals(Object obj) {
      return (obj instanceof ObjSub)
        && super.equals(obj)
        && this.z == ((ObjSub)obj).z;
    }
  }


  public static void main (String[] args) throws Exception {

    test();

  }

  public static void test() {

    if (true) {
      Arr arr = new Arr();
      arr.tryit (17);
    }

    if (true) {
      C c1 = new C("C1");
      c1.set_long (0L);
    }

    if (true) {
      java_check (1, 5);
    }

    if (true) {
      A a10 = new A("a10");
      A a11 = new A("a11");
      list_check (a10, a11);
    }

    B b1 = new B();
    b1.ecomp();
    b1.p (new A ("a0"));
    b1.comp();

    A a1 = new A("a1");
    a1.add (i);
    a1.add (j);
    a1.add();
    a1.tta();

    if (true) {
      A a2 = new A("a2");
      A a3 = new A("a3");
      A a4 = new A("a4");

      t1 (a1, a2, a3, a4);
    }
    if (sa1 == sa2) {
      if (verbose)
        out.println ("sa1 == sa2");
    } else {
      if (verbose)
        out.println ("sa1 != sa2");
    }
    double_check (1.2, 56, 1);

    D d1 = new D();
    d1.compare();

    E e1 = new E();
    e1.compare();

    F f1 = new F();
    f1.compare();
    F f2 = new F();
    f2.compare2();
    F f3 = new F();
    f3.compare3();

    G g = new G();
    g.compare();
  }

  public static void list_check (A a10, A a11) {

      List<A> list = new ArrayList<A>();
      list.add (a10);
      list.add (a11);
      list.contains (a11);
  }

  public static double double_check (double d1, Integer wrapper, int i1) {

    double loc1 = 22.4;
    double loc2 = loc1 + 14.6;

    d1 += loc2;
    i1 += loc1;

    return ((double) i1);
  }

  public static void t1 (A a1, A a2, A a3, A a4) {

    if (a1 == a2) {
      if (verbose)
        out.println ("a1 == a2");
    } else {
      if (verbose)
        out.println ("a1 != a2");
    }

    if (a1 != a2) {
      if (verbose)
        out.println ("a1 != a2");
    } else {
      if (verbose)
        out.println ("a1 == a2");
    }

    if (a1 == a1) {
      if (verbose)
        out.println ("a1 == a1");
    } else {
      if (verbose)
        out.println ("a1 != a1");
    }

    if (a2 != a2) {
      if (verbose)
        out.println ("a2 != a2");
    } else {
      if (verbose)
        out.println ("a2 == a2");
    }

    if (a3 == a3) {
      if (verbose)
        out.println ("a3 == a3");
    }
    if (a4 == a4) {
      if (verbose)
        out.println ("a4 == a4");
    }
  }

  public static int java_check (int i1, int i2) {
    return (Math.max (i1, i2));
  }
}
