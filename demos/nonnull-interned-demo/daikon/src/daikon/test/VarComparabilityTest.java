package daikon.test;

import junit.framework.*;

import daikon.*;

public class VarComparabilityTest extends TestCase {

  // for convenience
  public static void main(String[] args) {
    daikon.LogHelper.setupLogs (daikon.LogHelper.INFO);
    junit.textui.TestRunner.run(new TestSuite(LinearTernaryCoreTest.class));
  }

  public VarComparabilityTest(String name) {
    super(name);
  }

  static final int NONE = VarComparability.NONE;
  static final int IMPLICIT = VarComparability.IMPLICIT;

  boolean comp(VarComparability v1, VarComparability v2) {
    return VarComparability.comparable(v1, v2);
  }

  VarComparability parsei(String rep) {
    // count number of dimensions
    int dims = rep.split("\\[").length;
    return parsei(rep, dims);
  }

  VarComparability parsei(String rep, int dims) {
    String typerep = "int";
    for (int i=0; i<dims; i++) {
      typerep += "[]";
    }
    ProglangType type = ProglangType.parse(typerep);
    return VarComparability.parse(IMPLICIT, rep, type);
  }

  public void test_VarComparabilityImplicit_scalar() {
    VarComparability v1 = parsei("1");
    VarComparability v1a = parsei("1");
    VarComparability v2 = parsei("2");
    VarComparability v_1 = parsei("-1");
    // VarComparability v_1a = parsei("-1");
    VarComparability v_2 = parsei("-2");
    VarComparability v_3 = parsei("-3");

    assertTrue(comp(v1, v1));
    assertTrue(comp(v1, v1a));
    assertTrue(comp(v1a, v1));
    assertTrue(! comp(v1, v2));
    assertTrue(! comp(v2, v1));
    assertTrue(comp(v2, v2));

    assertTrue(comp(v1, v_1));
    assertTrue(comp(v1a, v_1));
    assertTrue(comp(v2, v_1));
    assertTrue(comp(v_1, v_1));
    assertTrue(comp(v_2, v_1));
    assertTrue(comp(v_3, v_1));

    assertTrue(comp(v1, v_2));
    assertTrue(comp(v1a, v_2));
    assertTrue(comp(v2, v_2));
    assertTrue(comp(v_1, v_2));
    assertTrue(comp(v_2, v_2));
    assertTrue(comp(v_3, v_2));

    assertTrue(comp(v1, v_3));
    assertTrue(comp(v1a, v_3));
    assertTrue(comp(v2, v_3));
    assertTrue(comp(v_1, v_3));
    assertTrue(comp(v_2, v_3));
    assertTrue(comp(v_3, v_3));
  }

  public void test_VarComparabilityImplicit_1Darray_parts() {

    VarComparability v1 = parsei("1");
    VarComparability v1a = parsei("1");
    VarComparability v2 = parsei("2");
    VarComparability v_1 = parsei("-1");
    VarComparability v_1a = parsei("-1");
    VarComparability v_2 = parsei("-2");
    VarComparability v_3 = parsei("-3");

    VarComparability v12 = parsei("1[2]");
    VarComparability v12a = parsei("1[2]");
    VarComparability v13 = parsei("1[3]");
    VarComparability v13a = parsei("1[3]");
    VarComparability v23 = parsei("2[3]");
    VarComparability v23a = parsei("2[3]");
    VarComparability v1_1 = parsei("1[-1]");
    VarComparability v_12 = parsei("-1[2]");
    VarComparability v_1_1 = parsei("-1[-1]");

    assertTrue(comp(v1, v12.elementType()));
    assertTrue(comp(v1, v13.elementType()));
    assertTrue(! comp(v1, v23.elementType()));
    assertTrue(comp(v1, v1_1.elementType()));
    assertTrue(comp(v1, v_1_1.elementType()));
    assertTrue(! comp(v1, v12.indexType(0)));
    assertTrue(! comp(v1, v13.indexType(0)));
    assertTrue(! comp(v1, v23.indexType(0)));
    assertTrue(comp(v1, v1_1.indexType(0)));
    assertTrue(comp(v1, v_1_1.indexType(0)));

    assertTrue(! comp(v2, v12.elementType()));
    assertTrue(! comp(v2, v13.elementType()));
    assertTrue(comp(v2, v23.elementType()));
    assertTrue(! comp(v2, v1_1.elementType()));
    assertTrue(comp(v2, v_1_1.elementType()));
    assertTrue(comp(v2, v12.indexType(0)));
    assertTrue(! comp(v2, v13.indexType(0)));
    assertTrue(! comp(v2, v23.indexType(0)));
    assertTrue(comp(v2, v1_1.indexType(0)));
    assertTrue(comp(v2, v_1_1.indexType(0)));

  }

  public void test_VarComparabilityImplicit_1Darray_whole() {
    VarComparability v12 = parsei("1[2]");
    VarComparability v12a = parsei("1[2]");
    VarComparability v13 = parsei("1[3]");
    VarComparability v13a = parsei("1[3]");
    VarComparability v23 = parsei("2[3]");
    VarComparability v23a = parsei("2[3]");
    VarComparability v1_1 = parsei("1[-1]");
    VarComparability v_12 = parsei("-1[2]");
    VarComparability v_1_1 = parsei("-1[-1]");

    assertTrue(comp(v12, v12));
    assertTrue(comp(v12, v12a));
    assertTrue(! comp(v12, v13));
    assertTrue(! comp(v12, v23));
    assertTrue(! comp(v13, v12));
    assertTrue(comp(v13, v13));
    assertTrue(comp(v13, v13a));
    assertTrue(! comp(v13, v23));
    assertTrue(! comp(v23, v12));
    assertTrue(! comp(v23, v13));
    assertTrue(comp(v23, v23));
    assertTrue(comp(v23, v23a));

    assertTrue(comp(v1_1, v12));
    assertTrue(comp(v1_1, v13));
    assertTrue(! comp(v1_1, v23));
    assertTrue(comp(v1_1, v1_1));
    assertTrue(comp(v1_1, v_12));
    assertTrue(comp(v1_1, v_1_1));

    assertTrue(comp(v_12, v12));
    assertTrue(! comp(v_12, v13));
    assertTrue(! comp(v_12, v23));
    assertTrue(comp(v_12, v1_1));
    assertTrue(comp(v_12, v_12));
    assertTrue(comp(v_12, v_1_1));

    assertTrue(comp(v_1_1, v12));
    assertTrue(comp(v_1_1, v13));
    assertTrue(comp(v_1_1, v23));
    assertTrue(comp(v_1_1, v1_1));
    assertTrue(comp(v_1_1, v_12));
    assertTrue(comp(v_1_1, v_1_1));
  }

  public void test_VarComparabilityImplicit_nDarray_whole() {
    VarComparability v12 = parsei("1[2]");
    VarComparability v12a = parsei("1[2]");
    VarComparability v123 = parsei("1[2][3]");
    VarComparability v123a = parsei("1[2][3]");
    VarComparability v1234 = parsei("1[2][3][4]");
    VarComparability v1234a = parsei("1[2][3][4]");
    VarComparability v_34 = parsei("-3[4]");
    VarComparability v_234 = parsei("-2[3][4]");
    VarComparability v_1234 = parsei("-1[2][3][4]");
    VarComparability v_1 = parsei("-1");

    assertTrue(comp(v12, v12));
    assertTrue(comp(v123, v123));
    assertTrue(comp(v1234, v1234));
    assertTrue(comp(v12a, v12));
    assertTrue(! comp(v12a, v123));
    assertTrue(! comp(v12a, v1234));
    assertTrue(! comp(v123a, v12));
    assertTrue(comp(v123a, v123));
    assertTrue(! comp(v123a, v1234));
    assertTrue(! comp(v1234a, v12));
    assertTrue(! comp(v1234a, v123));
    assertTrue(comp(v1234a, v1234));

    assertTrue(comp(v12, v123.elementType()));
    assertTrue(comp(v123, v1234.elementType()));

    assertTrue(comp(v_1, v12));
    assertTrue(comp(v_1, v123));
    assertTrue(comp(v_1, v1234));

    assertTrue(! comp(v_1234, v12));
    assertTrue(! comp(v_1234, v123));
    assertTrue(comp(v_1234, v1234));

    assertTrue(! comp(v_234, v12));
    assertTrue(! comp(v_234, v123));
    assertTrue(comp(v_234, v1234));

    assertTrue(! comp(v_34, v12));
    assertTrue(! comp(v_34, v123));
    assertTrue(comp(v_34, v1234));

  }

}
