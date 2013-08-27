package daikon.test.inv;

import junit.framework.*;
import daikon.*;
import daikon.config.*;
import daikon.inv.*;
import daikon.inv.binary.twoScalar.*;
import daikon.test.*;
import java.util.*;

public class InvariantTester extends TestCase {

  public static void main(String[] args) {
    daikon.LogHelper.setupLogs (daikon.LogHelper.INFO);
    junit.textui.TestRunner.run(new TestSuite(InvariantTester.class));
  }

  public InvariantTester(String name) {
    super(name);
  }

  public VarInfo newIntVarInfo(String name) {
    return new VarInfo(name,
                       ProglangType.INT,
                       ProglangType.INT,
                       VarComparabilityNone.it,
                       VarInfoAux.getDefault());
  }

  public void testClassVarnameComparator() {
    Comparator<Invariant> c = new Invariant.ClassVarnameComparator();

    VarInfo[] vars = { Common.makeIntVarInfo("x"), Common.makeIntVarInfo("y") };
    PptTopLevel ppt = Common.makePptTopLevel("Foo:::OBJECT", vars);
    PptSlice slice = new PptSlice2(ppt, vars);

    Invariant inv1, inv2, inv2_2, inv2_3, inv2_4, inv2_5, inv2_6, inv3, inv4, inv5, inv6;

    Configuration.getInstance().apply
      ("daikon.inv.binary.twoScalar.NumericInt.enabled = 1");

    inv1 = NumericInt.BitwiseComplement.get_proto().instantiate(slice);
    Assert.assertTrue(c.compare(inv1, inv1) == 0);

    inv2 = IntEqual.get_proto().instantiate(slice);
    inv2_2 = IntNonEqual.get_proto().instantiate(slice);
    inv2_3 = IntLessThan.get_proto().instantiate(slice);
    inv2_4 = IntLessEqual.get_proto().instantiate(slice);
    inv2_5 = IntGreaterThan.get_proto().instantiate(slice);
    inv2_6 = IntGreaterEqual.get_proto().instantiate(slice);
    Assert.assertTrue(c.compare(inv1, inv2) > 0);
    Assert.assertTrue(c.compare(inv1, inv2_2) > 0);
    Assert.assertTrue(c.compare(inv1, inv2_3) > 0);
    Assert.assertTrue(c.compare(inv1, inv2_4) > 0);
    Assert.assertTrue(c.compare(inv1, inv2_5) > 0);
    Assert.assertTrue(c.compare(inv1, inv2_6) > 0);

    inv3 = LinearBinary.get_proto().instantiate(slice);
    Assert.assertTrue(c.compare(inv3, inv1) < 0);

    inv4 = IntNonEqual.get_proto().instantiate(slice);
    Assert.assertTrue(c.compare(inv1, inv4) > 0);

    inv5 = Implication.makeImplication(ppt, inv1, inv2, false, null, null);
    inv6 = Implication.makeImplication(ppt, inv1, inv3, false, null, null);
    Assert.assertTrue(c.compare(inv5, inv6) < 0);

    inv5 = Implication.makeImplication(ppt, inv2, inv1, false, null, null);
    inv6 = Implication.makeImplication(ppt, inv2, inv3, false, null, null);
    Assert.assertTrue(c.compare(inv5, inv6) > 0);

    inv5 = Implication.makeImplication(ppt, inv3, inv2, false, null, null);
    inv6 = Implication.makeImplication(ppt, inv3, inv1, false, null, null);
    Assert.assertTrue(c.compare(inv5, inv6) < 0);

    inv5 = Implication.makeImplication(ppt, inv1, inv4, false, null, null);
    inv6 = Implication.makeImplication(ppt, inv3, inv4, false, null, null);
    Assert.assertTrue(c.compare(inv5, inv6) > 0);

    inv5 = Implication.makeImplication(ppt, inv2, inv4, false, null, null);
    inv6 = Implication.makeImplication(ppt, inv4, inv1, false, null, null);
    Assert.assertTrue(c.compare(inv5, inv6) < 0);

    VarInfo[] vars2 = { Common.makeIntVarInfo("x"), Common.makeIntVarInfo("z") };
    PptTopLevel ppt2 = Common.makePptTopLevel("Foo:::OBJECT", vars2);
    PptSlice slice2 = new PptSlice2(ppt2, vars2);
    inv2 = NumericInt.BitwiseComplement.get_proto().instantiate(slice2);
    Assert.assertTrue(c.compare(inv1, inv2) < 0);

    vars2[0] = Common.makeIntVarInfo("a");
    vars2[1] = Common.makeIntVarInfo("y");
    ppt2 = Common.makePptTopLevel("Foo:::OBJECT", vars2);
    slice2 = new PptSlice2(ppt2, vars2);
    inv2 = NumericInt.BitwiseComplement.get_proto().instantiate (slice2);
    Assert.assertTrue(c.compare(inv1, inv2) > 0);
  }

  public void test_prob_is_ge() {
    Assert.assertTrue(Invariant.prob_is_ge(0, 11) == 1);
    Assert.assertTrue(Invariant.prob_is_ge(1, 11) == 1);
    Assert.assertTrue(Invariant.prob_is_ge(2, 11) == .9);
    Assert.assertTrue(Invariant.prob_is_ge(3, 11) == .8);
    Assert.assertTrue(Invariant.prob_is_ge(9, 11) == .2);
    Assert.assertTrue(Invariant.prob_is_ge(10, 11) == .1);
    Assert.assertTrue(Invariant.prob_is_ge(11, 11) == 0);
    Assert.assertTrue(Invariant.prob_is_ge(20, 11) == 0);
  }

  public void test_prob_and() {

    Assert.assertTrue(Invariant.prob_and(0, 0) == 0);
    Assert.assertTrue(Invariant.prob_and(0, 1) == 1);
    Assert.assertTrue(Invariant.prob_and(1, 0) == 1);
    Assert.assertTrue(Invariant.prob_and(1, 1) == 1);
    Assert.assertTrue(Invariant.prob_and(0, .5) == .5);
    Assert.assertTrue(Invariant.prob_and(.5, 0) == .5);
    Assert.assertTrue(Invariant.prob_and(1, .5) == 1);
    Assert.assertTrue(Invariant.prob_and(.5, 1) == 1);
    Assert.assertTrue(Invariant.prob_and(0, .1) == .1);
    Assert.assertTrue(Invariant.prob_and(.1, 0) == .1);
    Assert.assertTrue(Invariant.prob_and(1, .1) == 1);
    Assert.assertTrue(Invariant.prob_and(.1, 1) == 1);
    Assert.assertTrue(Invariant.prob_and(.5, .5) == .75);
    Assert.assertTrue(Invariant.prob_and(.1, .9) == .91);
    Assert.assertTrue(Invariant.prob_and(.9, .1) == .91);

    Random r = new Random(20010907);
    for (int i=0; i<100; i++) {
      double x = r.nextDouble();
      double y = r.nextDouble();
      double z = r.nextDouble();
      double r1 = Invariant.prob_and(x, y, z);
      double r2 = Invariant.prob_and(x, Invariant.prob_and(y, z));
      double r3 = Invariant.prob_and(Invariant.prob_and(x, y), z);
      Assert.assertTrue(Math.abs(r1-r2) < .000001);
      Assert.assertTrue(Math.abs(r1-r3) < .000001);
      Assert.assertTrue(Math.abs(r2-r3) < .000001);
    }
  }

  public void test_prob_or() {
    Assert.assertTrue(Invariant.prob_or(0, 0) == 0);
    Assert.assertTrue(Invariant.prob_or(0, 1) == 0);
    Assert.assertTrue(Invariant.prob_or(1, 0) == 0);
    Assert.assertTrue(Invariant.prob_or(1, 1) == 1);
    Assert.assertTrue(Invariant.prob_or(0, .5) == 0);
    Assert.assertTrue(Invariant.prob_or(.5, 0) == 0);
    Assert.assertTrue(Invariant.prob_or(1, .5) == .5);
    Assert.assertTrue(Invariant.prob_or(.5, 1) == .5);
    Assert.assertTrue(Invariant.prob_or(0, .1) == 0);
    Assert.assertTrue(Invariant.prob_or(.1, 0) == 0);
    Assert.assertTrue(Invariant.prob_or(1, .1) == .1);
    Assert.assertTrue(Invariant.prob_or(.1, 1) == .1);
    Assert.assertTrue(Invariant.prob_or(.5, .5) == .5);
    Assert.assertTrue(Invariant.prob_or(.1, .9) == .1);
    Assert.assertTrue(Invariant.prob_or(.9, .1) == .1);
  }

}
