package daikon.test;

import junit.framework.*;
import daikon.*;
import java.util.Random;
import java.util.BitSet;

public class ModBitTrackerTest extends TestCase {

  public static void main(String[] args) {
    daikon.LogHelper.setupLogs (daikon.LogHelper.INFO);
    junit.textui.TestRunner.run(new TestSuite(ModBitTrackerTest.class));
  }

  public ModBitTrackerTest(String name) {
    super(name);
  }

  // Plan:
  // Create many different variables.  Give them a skewed distribution of
  // random modbits so that it takes them a while to separate.  In the end,
  // give them modbits so that they are all separated.  See if the results
  // are as they should be.

  // I should probably add some tests that test arguments other than 1 for
  // the "count" argument to ModBitTracker.add().

  private Random r = new Random(20031014L);

  private ModBitTracker makeModBitTracker(BitSet[] bitsets) {
    int numvars = bitsets.length;
    int numsamples = bitsets[0].size();

    ModBitTracker result = new ModBitTracker(numvars);

    Object[] vals = new Object[numvars];
    int[] mods = new int[numvars];
    // We'll reuse this ValueTuple throughout, side-effecting its mods array.
    ValueTuple vt = ValueTuple.makeUninterned(vals, mods);
    for (int sampleno=0; sampleno<numsamples; sampleno++) {
      for (int var=0; var<numvars; var++) {
        mods[var] = booleanToModBit(bitsets[var].get(sampleno));
      }
      result.add(vt, 1);
    }
    return result;
  }

  private void checkModBitTracker(ModBitTracker mbt, BitSet[] bitsets) {
    int numvars = bitsets.length;
    int numsamples = bitsets[0].size();

    assertEquals(mbt.num_vars(), numvars);
    assertEquals(mbt.num_samples(), numsamples);

    for (int i=0; i<numvars; i++) {
      assertEquals(bitsets[i], mbt.get(i));
    }
  }

  private int booleanToModBit(boolean b) {
    return (b ? ValueTuple.MODIFIED : ValueTuple.MISSING_NONSENSICAL);
  }

  private boolean modBitToBoolean(int mb) {
    return (mb == ValueTuple.MODIFIED) || (mb == ValueTuple.UNMODIFIED);
  }

  private boolean randomModBoolean(int varno, int sampleno) {
    boolean unusual = (r.nextInt(100) == 0);
    boolean result;
    if (varno % 2 == 0) {
      result = (varno % 2 == 0);
    } else {
      result = ((varno + sampleno) % 2 == 0);
    }
    if (unusual) {
      result = !result;
    }
    return result;
  }

  // Make numvars different unique random BitSets, each of size
  // numsamples+numvars.  Then add duplicate_factor more BitSets (e.g., if
  // duplicate_factor is 1, then double the number of BitSets), where each
  // of the additional ones is a duplicate of one of the unique ones.

  BitSet[] makeBitSets(int numvars, int numsamples, double duplicate_factor) {
    int totalvars = (int) (numvars * (1+duplicate_factor));
    BitSet[] result = new BitSet[totalvars];
    for (int var=0; var<numvars; var++) {
      BitSet bs = new BitSet(numsamples+numvars);
      for (int sample=0; sample<numsamples; sample++) {
        bs.set(sample, randomModBoolean(var, sample));
      }
      // add samples that make all the variables unique
      bs.set(numsamples+numvars-1, false);
      bs.set(numsamples+var, true);
      result[var] = bs;
    }
    // Add duplicate BitSets.
    for (int var=numvars; var<totalvars; var++) {
      result[var] = (BitSet) (result[r.nextInt(numvars)].clone());
    }
    return result;
  }


  public void oneModBitTrackerTest(int vars, int samples, double duplicate_factor) {
    BitSet[] bitsets = makeBitSets(vars, samples, duplicate_factor);
    ModBitTracker mbt = makeModBitTracker(bitsets);
    checkModBitTracker(mbt, bitsets);
    assertEquals(mbt.num_sets(), vars);
  }

  public void testModBitTracker() {
    oneModBitTrackerTest(1, 2, 0.0);
    oneModBitTrackerTest(2, 2, 0.0);
    oneModBitTrackerTest(5, 10, 0.0);
    oneModBitTrackerTest(100, 1000, 5.0);
  }

}
