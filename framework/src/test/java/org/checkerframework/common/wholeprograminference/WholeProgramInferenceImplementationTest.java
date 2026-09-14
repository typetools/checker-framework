package org.checkerframework.common.wholeprograminference;

import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Impure;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link WholeProgramInferenceImplementation}. */
public class WholeProgramInferenceImplementationTest {

  /** The fully-qualified name of {@link Pure}. */
  private static final String PURE = Pure.class.getCanonicalName();

  /** The fully-qualified name of {@link SideEffectFree}. */
  private static final String SIDE_EFFECT_FREE = SideEffectFree.class.getCanonicalName();

  /** The fully-qualified name of {@link Deterministic}. */
  private static final String DETERMINISTIC = Deterministic.class.getCanonicalName();

  /** The fully-qualified name of {@link Impure}. */
  private static final String IMPURE = Impure.class.getCanonicalName();

  /** The fully-qualified name of an annotation that is not a purity annotation. */
  private static final String NOT_A_PURITY_ANNOTATION =
      "org.checkerframework.checker.nullness.qual.NonNull";

  /**
   * Tests every pair of purity annotations. Pure is the bottom of the lattice, SideEffectFree and
   * Deterministic are incomparable siblings above it, and Impure is the top.
   */
  @Test
  public void lubOfPurityAnnotations() {
    assertLub(PURE, PURE, PURE);
    assertLub(SIDE_EFFECT_FREE, PURE, SIDE_EFFECT_FREE);
    assertLub(DETERMINISTIC, PURE, DETERMINISTIC);
    assertLub(IMPURE, PURE, IMPURE);
    assertLub(SIDE_EFFECT_FREE, SIDE_EFFECT_FREE, SIDE_EFFECT_FREE);
    assertLub(IMPURE, SIDE_EFFECT_FREE, DETERMINISTIC);
    assertLub(IMPURE, SIDE_EFFECT_FREE, IMPURE);
    assertLub(DETERMINISTIC, DETERMINISTIC, DETERMINISTIC);
    assertLub(IMPURE, DETERMINISTIC, IMPURE);
    assertLub(IMPURE, IMPURE, IMPURE);
  }

  /** Tests the "fail-safe" behavior for an argument that is not a purity annotation. */
  @Test
  public void lubOfNonPurityAnnotation() {
    assertLub(IMPURE, NOT_A_PURITY_ANNOTATION, PURE);
    assertLub(IMPURE, NOT_A_PURITY_ANNOTATION, SIDE_EFFECT_FREE);
    assertLub(IMPURE, NOT_A_PURITY_ANNOTATION, DETERMINISTIC);
    assertLub(IMPURE, NOT_A_PURITY_ANNOTATION, IMPURE);
    assertLub(IMPURE, NOT_A_PURITY_ANNOTATION, NOT_A_PURITY_ANNOTATION);
  }

  /**
   * Asserts that the least upper bound of {@code anno1Name} and {@code anno2Name} is {@code
   * expected}, regardless of the order of the two arguments.
   *
   * @param expected the fully-qualified name of the expected least upper bound
   * @param anno1Name the fully-qualified name of an annotation
   * @param anno2Name the fully-qualified name of another annotation
   */
  private static void assertLub(String expected, String anno1Name, String anno2Name) {
    Assert.assertEquals(
        expected,
        WholeProgramInferenceImplementation.lubPurityAnnotationNames(anno1Name, anno2Name));
    Assert.assertEquals(
        "lubPurityAnnotationNames is not commutative",
        expected,
        WholeProgramInferenceImplementation.lubPurityAnnotationNames(anno2Name, anno1Name));
  }
}
