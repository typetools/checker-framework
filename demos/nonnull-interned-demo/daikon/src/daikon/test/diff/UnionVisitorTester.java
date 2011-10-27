package daikon.test.diff;

import java.util.*;
import junit.framework.*;
import daikon.*;
import daikon.diff.*;
import daikon.inv.*;
import daikon.test.*;

public class UnionVisitorTester extends TestCase {

  private Diff diff = new Diff(true);

  public static void main(String[] args) {
    daikon.LogHelper.setupLogs (LogHelper.INFO);
    junit.textui.TestRunner.run(new TestSuite(UnionVisitorTester.class));
  }

  public UnionVisitorTester(String name) {
    super(name);
  }

  // X1 and X2 have the same class and vars, but different formula
  // M_<num> and N_<num> have the same class, vars, and formula, but
  // different probabilities
  // map1: A->{W, X1, Y}, B->{Y}, D->{M_001, N_001, O_1}
  // map2: A->{W, X2, Z}, C->{Z}, D->{M_1, N_0001}
  // map1 union map2: A->{W, X1, X2, Y, Z}, B->{Y}, C->{Z},
  //                  D->{M_001, N_0001, O_1}
  public void testXor() {
    VarInfo[] vars = { DiffTester.newIntVarInfo("w"),
                       DiffTester.newIntVarInfo("x"),
                       DiffTester.newIntVarInfo("y"),
                       DiffTester.newIntVarInfo("z"),
                       DiffTester.newIntVarInfo("m"),
                       DiffTester.newIntVarInfo("n"),
                       DiffTester.newIntVarInfo("o"),
    };
    PptTopLevel A = Common.makePptTopLevel("A:::OBJECT", vars);
    PptTopLevel B = Common.makePptTopLevel("B:::OBJECT", vars);
    PptTopLevel C = Common.makePptTopLevel("C:::OBJECT", vars);
    PptTopLevel D = Common.makePptTopLevel("D:::OBJECT", vars);
    PptSlice slicew = new PptSlice1(A, new VarInfo[] {vars[0]});
    PptSlice slicex = new PptSlice1(A, new VarInfo[] {vars[1]});
    PptSlice slicey = new PptSlice1(A, new VarInfo[] {vars[2]});
    PptSlice slicez = new PptSlice1(A, new VarInfo[] {vars[3]});
    PptSlice slicem = new PptSlice1(A, new VarInfo[] {vars[4]});
    PptSlice slicen = new PptSlice1(A, new VarInfo[] {vars[5]});
    PptSlice sliceo = new PptSlice1(A, new VarInfo[] {vars[6]});
    Invariant W = new DiffDummyInvariant(slicew, "W", true);
    Invariant X1 = new DiffDummyInvariant(slicex, "X1", true);
    Invariant X2 = new DiffDummyInvariant(slicex, "X2", true);
    Invariant Y = new DiffDummyInvariant(slicey, "Y", true);
    Invariant Z = new DiffDummyInvariant(slicez, "Z", true);
    Invariant M_001 = new DiffDummyInvariant(slicem, "M", .999);
    Invariant M_1 = new DiffDummyInvariant(slicem, "M", 0);
    Invariant N_001 = new DiffDummyInvariant(slicen, "N", .999);
    Invariant N_0001 = new DiffDummyInvariant(slicen, "N", .9999);
    Invariant O_1 = new DiffDummyInvariant(sliceo, "O", 0);

    InvMap map1 = new InvMap();
    map1.put(A, Arrays.asList(new Invariant[] {W, X1, Y}));
    map1.put(B, Arrays.asList(new Invariant[] {Y}));
    map1.put(D, Arrays.asList(new Invariant[] {M_001, N_001, O_1}));

    InvMap map2 = new InvMap();
    map2.put(A, Arrays.asList(new Invariant[] {W, X2, Z}));
    map2.put(C, Arrays.asList(new Invariant[] {Z}));
    map2.put(D, Arrays.asList(new Invariant[] {M_1, N_0001}));

    diff.setAllInvComparators(new Invariant.ClassVarnameFormulaComparator());
    RootNode root = diff.diffInvMap(map1, map2);
    UnionVisitor v = new UnionVisitor();
    root.accept(v);
    InvMap result = v.getResult();

    InvMap expected = new InvMap();
    expected.put(A, Arrays.asList(new Invariant[] {W, X1, X2, Y, Z}));
    expected.put(B, Arrays.asList(new Invariant[] {Y}));
    expected.put(C, Arrays.asList(new Invariant[] {Z}));
    expected.put(D, Arrays.asList(new Invariant[] {M_001, N_0001, O_1}));

    assertEquals(expected.toString(), result.toString());
  }

}
