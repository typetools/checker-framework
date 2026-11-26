package org.checkerframework.framework.test.junit;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.EqualityAtmComparer;
import org.checkerframework.framework.type.visitor.EquivalentAtmComboScanner;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests of {@link EquivalentAtmComboScanner#scan(Iterable, Iterable, Object)}, which scans two
 * lists of types, such as the parameter types of two executable types.
 *
 * <p>The tests use an {@link EqualityAtmComparer} whose comparison of each pair of types is
 * scripted, so they test how the results for the pairs are combined, not how a pair of types is
 * compared.
 */
public class EquivalentAtmComboScannerTest {

  /**
   * Tests that scanning two lists of types combines the results for every pair of corresponding
   * elements rather than keeping only the result for the last pair.
   */
  @Test
  public void scanCombinesEveryElement() {
    // Only the first of two pairs differs.
    Assert.assertEquals(false, scanWithResults(2, 2, false, true));
    // Only the second of two pairs differs.
    Assert.assertEquals(false, scanWithResults(2, 2, true, false));
    // No pair differs.
    Assert.assertEquals(true, scanWithResults(2, 2, true, true));
    Assert.assertEquals(true, scanWithResults(1, 1, true));
    Assert.assertEquals(false, scanWithResults(1, 1, false));
    Assert.assertEquals(false, scanWithResults(3, 3, false, true, true));
  }

  /** Tests that scanning two lists with no corresponding elements yields null. */
  @Test
  public void scanEmpty() {
    Assert.assertNull(scanWithResults(0, 0));
    Assert.assertNull(scanWithResults(0, 2));
    Assert.assertNull(scanWithResults(2, 0));
  }

  /** Tests that scanning two lists of different lengths ignores the extra elements. */
  @Test
  public void scanDifferentLengths() {
    Assert.assertEquals(false, scanWithResults(1, 2, false));
    Assert.assertEquals(false, scanWithResults(3, 2, true, false));
    Assert.assertEquals(true, scanWithResults(2, 3, true, true));
  }

  /**
   * Returns the result of scanning two lists of the given lengths whose corresponding elements
   * compare with the given results. Fails if the scan does not compare exactly {@code
   * results.length} pairs.
   *
   * @param length1 the length of the first list
   * @param length2 the length of the second list
   * @param results the result of comparing each pair of corresponding elements
   * @return the result of scanning the two lists
   */
  private static @Nullable Boolean scanWithResults(int length1, int length2, Boolean... results) {
    // The scanner does not look at the elements, so they can be null.
    List<AnnotatedTypeMirror> types1 = Arrays.asList(new AnnotatedTypeMirror[length1]);
    List<AnnotatedTypeMirror> types2 = Arrays.asList(new AnnotatedTypeMirror[length2]);
    return new ScriptedComparer(results).scanLists(types1, types2);
  }

  /** An {@link EqualityAtmComparer} whose comparison of each pair of types is given in advance. */
  private static class ScriptedComparer extends EqualityAtmComparer {

    /** The results to return from successive comparisons of a pair of types. */
    private final Deque<Boolean> results;

    /**
     * Creates a comparer whose successive comparisons of a pair of types return {@code results}.
     *
     * @param results the results of successive comparisons
     */
    ScriptedComparer(Boolean... results) {
      this.results = new ArrayDeque<>(Arrays.asList(results));
    }

    @Override
    protected Boolean scan(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, Void v) {
      Assert.assertFalse("more pairs were scanned than expected", results.isEmpty());
      return results.remove();
    }

    /**
     * Returns the result of scanning corresponding elements of two lists. Fails if the scan does
     * not use every result that was given to the constructor.
     *
     * @param types1 a list of types
     * @param types2 a list of types
     * @return the result of scanning the lists
     */
    @Nullable Boolean scanLists(
        List<AnnotatedTypeMirror> types1, List<AnnotatedTypeMirror> types2) {
      Boolean result = scan(types1, types2, null);
      Assert.assertTrue("not every pair was scanned", results.isEmpty());
      return result;
    }
  }
}
