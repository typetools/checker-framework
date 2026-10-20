package org.checkerframework.framework.stub;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.AnnotationExpr;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link RemoveAnnotationsForInference}. */
public class RemoveAnnotationsForInferenceTest {

  /** The fully-qualified name of an annotation that the Nullness Checker issues warnings about. */
  private static final String NON_NULL = "org.checkerframework.checker.nullness.qual.NonNull";

  /** The fully-qualified name of an annotation that the Interning Checker issues warnings about. */
  private static final String INTERNED = "org.checkerframework.checker.interning.qual.Interned";

  /** Tests a suppression string that consists of a checker name alone, with no message key. */
  @Test
  public void suppressesWithoutMessageKey() {
    assertSuppresses(true, "@SuppressWarnings(\"nullness\")", NON_NULL);
    assertSuppresses(true, "@SuppressWarnings(\"allcheckers\")", NON_NULL);
    assertSuppresses(false, "@SuppressWarnings(\"interning\")", NON_NULL);
  }

  /**
   * Tests a suppression string of the form {@code checkername:messagekey}. Only the part before the
   * colon names a checker, so only that part is compared against the annotation's name.
   */
  @Test
  public void suppressesWithMessageKey() {
    assertSuppresses(true, "@SuppressWarnings(\"nullness:assignment\")", NON_NULL);
    assertSuppresses(true, "@SuppressWarnings(\"allcheckers:purity\")", NON_NULL);
    assertSuppresses(false, "@SuppressWarnings(\"interning:not.interned\")", NON_NULL);
    // The message key must not be treated as a checker name, even when it looks like one.
    assertSuppresses(false, "@SuppressWarnings(\"interning:nullness\")", NON_NULL);
  }

  /** Tests a suppressor that contains more than one suppression string. */
  @Test
  public void suppressesWithMultipleStrings() {
    String suppressor = "@SuppressWarnings({\"interning:not.interned\", \"nullness:assignment\"})";
    assertSuppresses(true, suppressor, NON_NULL);
    assertSuppresses(true, suppressor, INTERNED);
    assertSuppresses(false, suppressor, "org.checkerframework.checker.signedness.qual.Signed");
  }

  /**
   * Tests an annotation that suppresses all warnings without being a {@code SuppressWarnings}
   * annotation, and an annotation that suppresses no warnings at all.
   */
  @Test
  public void suppressesWithOtherAnnotations() {
    assertSuppresses(true, "@IgnoreInWholeProgramInference", NON_NULL);
    assertSuppresses(true, "@Option(\"an option\")", NON_NULL);
    assertSuppresses(false, "@Deprecated", NON_NULL);
  }

  /**
   * Tests a suppressee that is given as the several fully-qualified names that an unqualified
   * annotation name might stand for.
   */
  @Test
  public void suppressesAmbiguousSuppressee() {
    AnnotationExpr suppressor =
        StaticJavaParser.parseAnnotation("@SuppressWarnings(\"nullness:assignment\")");
    Assert.assertTrue(
        RemoveAnnotationsForInference.suppresses(suppressor, Arrays.asList(INTERNED, NON_NULL)));
  }

  /**
   * Asserts that {@code RemoveAnnotationsForInference.suppresses} returns {@code expected} for the
   * given annotations.
   *
   * @param expected the expected result
   * @param suppressor the source code of an annotation that might suppress warnings
   * @param suppressee the fully-qualified name of an annotation about which warnings might be
   *     suppressed
   */
  private static void assertSuppresses(boolean expected, String suppressor, String suppressee) {
    AnnotationExpr suppressorExpr = StaticJavaParser.parseAnnotation(suppressor);
    Collection<String> suppressees = Collections.singletonList(suppressee);
    Assert.assertEquals(
        suppressor + " on " + suppressee,
        expected,
        RemoveAnnotationsForInference.suppresses(suppressorExpr, suppressees));
  }
}
