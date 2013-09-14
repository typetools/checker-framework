package daikon.test.diff;

import junit.framework.*;
import daikon.*;
import daikon.diff.*;
import daikon.inv.Invariant;

import java.util.*;

public class InvMapTester extends TestCase {

  private InvMap map;
  private PptTopLevel pptA = new PptTopLevel("A:::OBJECT", new VarInfo[0]);
  private PptTopLevel pptB = new PptTopLevel("A:::OBJECT", new VarInfo[0]);
  private PptTopLevel pptC = new PptTopLevel("A:::OBJECT", new VarInfo[0]);
  private List<Invariant> invsA = new ArrayList<Invariant>();
  private List<Invariant> invsB = new ArrayList<Invariant>();
  private List<Invariant> invsC = new ArrayList<Invariant>();

  public static void main(String[] args) {
    daikon.LogHelper.setupLogs(LogHelper.INFO);
    junit.textui.TestRunner.run(new TestSuite(InvMapTester.class));
  }

  public InvMapTester(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    super.setUp();
    map = new InvMap();
  }

  public void testABC() {
    map.put(pptA, invsA);
    map.put(pptB, invsB);
    map.put(pptC, invsC);

    // Test the keys
    Iterator<PptTopLevel> i = map.pptIterator();
    assertTrue(pptA == i.next());
    assertTrue(pptB == i.next());
    assertTrue(pptC == i.next());
    assertTrue(!i.hasNext());

    // Test the values
    assertTrue(invsA == map.get(pptA));
    assertTrue(invsB == map.get(pptB));
    assertTrue(invsC == map.get(pptC));
  }

  public void testCAB() {
    map.put(pptC, invsC);
    map.put(pptA, invsA);
    map.put(pptB, invsB);

    // Test the keys
    Iterator<PptTopLevel> i = map.pptIterator();
    assertTrue(pptC ==i.next());
    assertTrue(pptA == i.next());
    assertTrue(pptB == i.next());
    assertTrue(!i.hasNext());

    // Test the values
    assertTrue(invsA == map.get(pptA));
    assertTrue(invsB == map.get(pptB));
    assertTrue(invsC == map.get(pptC));
  }

}
