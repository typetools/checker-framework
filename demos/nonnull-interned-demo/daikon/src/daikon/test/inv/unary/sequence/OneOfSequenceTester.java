package daikon.test.inv.unary.sequence;

import junit.framework.*;
import daikon.*;
import daikon.inv.unary.sequence.*;
import utilMDE.*;
import daikon.test.Common;

public class OneOfSequenceTester extends TestCase {

  private VarInfo[] vars = { Common.makeHashcodeArrayVarInfo("x"),
                             Common.makeIntArrayVarInfo("y") };
  private PptTopLevel ppt = Common.makePptTopLevel("Foo.Baa(int):::ENTER", vars);
  private PptSlice slicex = new PptSlice1(ppt, new VarInfo[] {vars[0]});
  private PptSlice slicey = new PptSlice1(ppt, new VarInfo[] {vars[1]});

  private static final int DOESNT_MATTER = 0;

  public static void main(String[] args) {
    daikon.LogHelper.setupLogs (daikon.LogHelper.INFO);
    junit.textui.TestRunner.run(new TestSuite(OneOfSequenceTester.class));
  }

  public static VarInfo newIntArrayVarInfo(String name) {
    VarInfo result = new VarInfo(name,
                                 ProglangType.INT_ARRAY,
                                 ProglangType.INT_ARRAY,
                                 VarComparabilityNone.it,
                                 VarInfoAux.getDefault());
    return result;
  }

  public static VarInfo newHashcodeArrayVarInfo(String name) {
    VarInfo result = new VarInfo(name,
                                 ProglangType.HASHCODE_ARRAY,
                                 ProglangType.HASHCODE_ARRAY,
                                 VarComparabilityNone.it,
                                 VarInfoAux.getDefault());
    return result;
  }

  public OneOfSequenceTester(String name) {
    super(name);
  }

  public void testNonNonNonNull() {
    OneOfSequence inv1 = (OneOfSequence) OneOfSequence.get_proto().instantiate(slicex);
    OneOfSequence inv2 = (OneOfSequence)OneOfSequence.get_proto().instantiate(slicex);

    inv1.add_modified(Intern.intern(new long[] {19,23}), DOESNT_MATTER);
    inv2.add_modified(Intern.intern(new long[] {91,0}), DOESNT_MATTER);

    assertTrue(! inv1.isSameFormula(inv2));
  }

  public void testNonNonNonNon() {
    OneOfSequence inv1 = (OneOfSequence)OneOfSequence.get_proto().instantiate(slicex);
    OneOfSequence inv2 = (OneOfSequence)OneOfSequence.get_proto().instantiate(slicex);

    inv1.add_modified(Intern.intern(new long[] {19,23}), DOESNT_MATTER);
    inv2.add_modified(Intern.intern(new long[] {91,32}), DOESNT_MATTER);

    assertTrue(inv1.isSameFormula(inv2));
  }

  public void testNonNullNonNull() {
    OneOfSequence inv1 = (OneOfSequence)OneOfSequence.get_proto().instantiate(slicex);
    OneOfSequence inv2 = (OneOfSequence)OneOfSequence.get_proto().instantiate(slicex);

    inv1.add_modified(Intern.intern(new long[] {19,0}), DOESNT_MATTER);
    inv2.add_modified(Intern.intern(new long[] {91,0}), DOESNT_MATTER);

    assertTrue(inv1.isSameFormula(inv2));
  }

  public void testNullNullNullNull() {
    OneOfSequence inv1 = (OneOfSequence)OneOfSequence.get_proto().instantiate(slicex);
    OneOfSequence inv2 = (OneOfSequence)OneOfSequence.get_proto().instantiate(slicex);

    inv1.add_modified(Intern.intern(new long[] {0,0}), DOESNT_MATTER);
    inv2.add_modified(Intern.intern(new long[] {0,0}), DOESNT_MATTER);

    assertTrue(inv1.isSameFormula(inv2));
  }

  public void testDifferentLengths() {
    OneOfSequence inv1 = (OneOfSequence)OneOfSequence.get_proto().instantiate(slicex);
    OneOfSequence inv2 = (OneOfSequence)OneOfSequence.get_proto().instantiate(slicex);

    inv1.add_modified(Intern.intern(new long[] {0,0,0}), DOESNT_MATTER);
    inv2.add_modified(Intern.intern(new long[] {0,0}), DOESNT_MATTER);

    assertTrue(!inv1.isSameFormula(inv2));
  }

}
