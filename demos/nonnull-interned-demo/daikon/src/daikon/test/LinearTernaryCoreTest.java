package daikon.test;

import junit.framework.*;

import daikon.*;
import daikon.inv.OutputFormat;
import daikon.inv.ternary.threeScalar.*;

public class LinearTernaryCoreTest
  extends TestCase
{

  // for convenience
  public static void main(String[] args) {
    daikon.LogHelper.setupLogs (daikon.LogHelper.INFO);
    junit.textui.TestRunner.run(new TestSuite(LinearTernaryCoreTest.class));
  }

  public LinearTernaryCoreTest(String name) {
    super(name);
  }

  void set_cache(LinearTernaryCore ltc, int index, long x, long y, long z) {
    ltc.def_points[index] = new LinearTernaryCore.Point (x, y, z);
  }

  void one_test_set_tri_linear(int[][] triples, long goal_a, long goal_b, long goal_c, long goal_d) {
    LinearTernaryCore ltc = new LinearTernaryCore(null);
    for (int i=0; i<triples.length; i++) {
      assertTrue(triples[i].length == 3);
      set_cache (ltc, i, triples[i][0], triples[i][1], triples[i][2]);
    }
    double[] coef;
    try {
      coef = ltc.calc_tri_linear (ltc.def_points);
    } catch (ArithmeticException e) {
      // In the future, we should perhaps test triples that that don't
      // determine a plane; but none of the current ones do.
      throw new Error("Not reached");
      // assertTrue(false);
      // coef = null; // not reached
    }
   //  System.out.println("goals: " + goal_a + " " + goal_b + " " + goal_c + " " + goal_d);
   //  System.out.println("actual: " + coef[0] + " " + coef[1] + " " + coef[2] + " " + coef[3]);
    // System.out.println("difference: " + (goal_a - ltc.a) + " " + (goal_b - ltc.b) + " " + (goal_c - ltc.c));
    assertTrue(coef[0] == goal_a && coef[1] == goal_b && coef[2] == goal_c && coef[3] == goal_d);
  }

  public void test_set_tri_linear() {
    one_test_set_tri_linear(new int[][] { { 1, 2, 1 },
                                          { 2, 1, 7 },
                                          { 3, 3, 7 } },
                            4, -2, -1, 1);
    //     # like the above, but swap y and z; results in division-by-zero problem
    //     # tri_linear_relationship((1,1,2),(2,7,1),(3,7,3))
    one_test_set_tri_linear(new int[][] { { 1, 2, 6 },
                                          { 2, 1, -4 },
                                          { 3, 3, 7 } },
                        //    -3, 7, -1, -5);
                        3,-7,1, 5);

    // These have non-integer parameters; must have a LinearTernaryCoreFloat
    // in order to handle them.
    //
    // // a - 3 b + 2 c = -9.5
    // // 0.5 a + 4 b - 10 c = 9
    // // 3 a + 0.1 b + 2 c = -2.2
    // //   solution = -9.5, 9, -2.2
    // // Restated:
    // // .5 a - 1.5 b + c = -4.75
    // // -0.05 a - .4 b + c = -.9
    // // 1.5 a + 0.05 b + c = -1.1
    // //   solution = -9.5, 9, -2.2
    // one_test_set_tri_linear(new float[][] { { .5, -1.5, -4.75 },
    //                                       { -0.05, -.4, -.9 },
    //                                       { 1.5, 0.05, -1.1 } },
    //                         -9.5, 9, -2.2);
    //
    // // Another example:
    // //   2x + 3y + 1/3z = 10
    // //      3x + 4y + 1z = 17
    // //      2y + 7z = 46
    // //
    // //   Solution:
    // //   x = 1
    // //      y = 2
    // //      z = 6
  }

  public void one_test_format(double a, double b, double c, double d, String goal_result) {
    LinearTernaryCore ltc = new LinearTernaryCore(null);
    ltc.a = a;
    ltc.b = b;
    ltc.c = c;
    ltc.d = d;
    String actual_result = ltc.format_using(OutputFormat.DAIKON,
                                            "x", "y", "z");
 //    System.out.println("Expecting: " + goal_result);
 //    System.out.println("Actual:    " + actual_result);
     assertTrue(actual_result.equals(goal_result));
  }

  public void test_format() {
    // Need tests with all combinations of: integer/noninteger, and values
    // -1,0,1,other.
    one_test_format(1, 2, 1, 3, "x + 2 * y + z + 3 == 0");
    one_test_format(-1, 2, 1, 3, "- x + 2 * y + z + 3 == 0");
    one_test_format(-1, -2, -1, 3, "- x - 2 * y - z + 3 == 0");
    one_test_format(-1, -2, -4, -3, "- x - 2 * y - 4 * z - 3 == 0");
    one_test_format(-1, 2, 3, 0, "- x + 2 * y + 3 * z == 0");
    one_test_format(-1, 0, 0, 3, "- x + 3 == 0");
    one_test_format(0, -2, 5, -3, "- 2 * y + 5 * z - 3 == 0");
    one_test_format(-1, 1, -2, 0, "- x + y - 2 * z == 0");
    one_test_format(-1, -1, 2, 3, "- x - y + 2 * z + 3 == 0");
    one_test_format(3, -2, 0, -3, "3 * x - 2 * y - 3 == 0");
    //hmmm, we can't actually have this test because there are never any double coeffs, they're not
    //calculated as such and are converted to ints
  //  one_test_format(3.2, -2.2, 1.4, -3.4, "3.2 * x - 2.2 * y + 1.4 * z - 3.4 == 0");
    one_test_format(3.0, -2.0, 2.0, -3.0, "3 * x - 2 * y + 2 * z - 3 == 0");
    one_test_format(-1.0, 1.0, 0.0, 0.0, "- x + y == 0");
  }
}
