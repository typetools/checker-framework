package org.checkerframework.framework.test.junit;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.EqualityAtmComparer;
import org.checkerframework.framework.type.visitor.EquivalentAtmComboScanner;
import org.junit.Assert;
import org.junit.Test;

/** Tests of {@link EquivalentAtmComboScanner}. */
public class EquivalentAtmComboScannerTest {

  /**
   * Tests that scanning two lists of types, such as the parameter types of two executable types,
   * combines the results for every pair of elements rather than keeping only the last one. If it
   * kept only the last one, then {@link AnnotatedTypeMirror#equals} would consider two method types
   * equal when they differ only in their first formal parameter.
   */
  @Test
  public void scanCombinesEveryElement() {
    // Only the first of two pairs differs.
    Assert.assertFalse(scanWithResults(false, true));
    // Only the second of two pairs differs.
    Assert.assertFalse(scanWithResults(true, false));
    // No pair differs.
    Assert.assertTrue(scanWithResults(true, true));
    Assert.assertTrue(scanWithResults(true));
    Assert.assertFalse(scanWithResults(false));
    Assert.assertFalse(scanWithResults(false, true, true));
  }

  /**
   * Scans two lists whose elements compare with the given results, and returns the result of the
   * scan.
   *
   * @param results the result of comparing each pair of corresponding elements
   * @return the result of scanning the two lists
   */
  private static boolean scanWithResults(Boolean... results) {
    // The scanner does not look at the elements, so they can be null.
    List<AnnotatedTypeMirror> types = Arrays.asList(new AnnotatedTypeMirror[results.length]);
    return new ScriptedComparer(results).scanLists(types, types);
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
      return results.remove();
    }

    /**
     * Scans corresponding elements of two lists.
     *
     * @param types1 a list of types
     * @param types2 a list of types
     * @return the result of scanning the lists
     */
    boolean scanLists(List<AnnotatedTypeMirror> types1, List<AnnotatedTypeMirror> types2) {
      boolean result = scan(types1, types2, null);
      Assert.assertTrue("not every pair was scanned", results.isEmpty());
      return result;
    }
  }
}
