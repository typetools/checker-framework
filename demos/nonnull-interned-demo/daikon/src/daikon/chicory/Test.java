package daikon.chicory;

import static java.lang.System.out;
import java.io.*;

class Test {

  int t1 = 55;

  //public Test() {
  // }

  public static void main (String[] args) {

    Test t = new Test();
    t.t1 = 5;
    int i = t.sample(0);
    out.format ("sample return [35]   = %d%n", i);

    t = t.sample1();
    out.format ("sample return [32]   = %d%n", t.t1);

    double d = t.sample2();
    out.println ("sample return [62.4] = " + d);

    t.test_d (1.0, 5.0);
  }

  public Test[] test_array() {
    return (null);
  }

  public double test_d (double d1, double d2) {
    return (d1 * d2);
  }

  public int sample (int myint) {

    double my_d = t1;

    if (t1 == 6)
      return (int) my_d + 7;
    else
      return (7 * (int) my_d);
  }

  public Test sample1() {

    Test test1 = new Test();
    test1.t1 = 32;
    return test1;
  }

  public double sample2() {

    if (t1 == 6)
      return (5.43);
    else
      return (62.4);
  }
}
