package org.checkerframework.checker.testchecker.lubglb;

import java.util.Arrays;
import java.util.LinkedHashSet;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nullness.KeyForAnnotatedTypeFactory;
import org.checkerframework.checker.nullness.KeyForSubchecker;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * Tests the LUB, GLB, and canonicalization of the KeyFor Checker, whose qualifier hierarchy is a
 * {@code SubtypeIsSupersetQualifierHierarchy}. The LUB of two {@code @KeyFor} annotations is the
 * intersection of their values, and the GLB is the union. The canonical form of a {@code @KeyFor}
 * annotation has sorted values without duplicates.
 */
public class KeyForLubGlbChecker extends KeyForSubchecker {

  /**
   * Returns a {@code @KeyFor} annotation with the given values, in the given order.
   *
   * @param values the values of the annotation
   * @return a {@code @KeyFor} annotation with the given values
   */
  private AnnotationMirror keyFor(String... values) {
    AnnotationBuilder builder = new AnnotationBuilder(processingEnv, KeyFor.class);
    builder.setValue("value", values);
    return builder.build();
  }

  /**
   * Throws an exception if lub(arg1, arg2) != expected.
   *
   * @param arg1 the first argument
   * @param arg2 the second argument
   * @param expected the expected result
   */
  private void lubAssert(AnnotationMirror arg1, AnnotationMirror arg2, AnnotationMirror expected) {
    QualifierHierarchy qualHierarchy =
        ((BaseTypeVisitor<?>) visitor).getTypeFactory().getQualifierHierarchy();
    AnnotationMirror result = qualHierarchy.leastUpperBoundQualifiersOnly(arg1, arg2);
    if (!AnnotationUtils.areSame(expected, result)) {
      throw new AssertionError(
          String.format("LUB of %s and %s should be %s, but is %s", arg1, arg2, expected, result));
    }
  }

  /**
   * Throws an exception if glb(arg1, arg2) != expected.
   *
   * @param arg1 the first argument
   * @param arg2 the second argument
   * @param expected the expected result
   */
  private void glbAssert(AnnotationMirror arg1, AnnotationMirror arg2, AnnotationMirror expected) {
    QualifierHierarchy qualHierarchy =
        ((BaseTypeVisitor<?>) visitor).getTypeFactory().getQualifierHierarchy();
    AnnotationMirror result = qualHierarchy.greatestLowerBoundQualifiersOnly(arg1, arg2);
    if (!AnnotationUtils.areSame(expected, result)) {
      throw new AssertionError(
          String.format("GLB of %s and %s should be %s, but is %s", arg1, arg2, expected, result));
    }
  }

  /**
   * Throws an exception if {@code actual} is not the same as {@code expected}.
   *
   * @param description a description of how {@code actual} was computed
   * @param actual the actual result
   * @param expected the expected result
   */
  private void sameAssert(String description, AnnotationMirror actual, AnnotationMirror expected) {
    if (!AnnotationUtils.areSame(expected, actual)) {
      throw new AssertionError(
          String.format("%s should be %s, but is %s", description, expected, actual));
    }
  }

  @Override
  public void initChecker() {
    super.initChecker();

    AnnotationMirror a = keyFor("a");
    AnnotationMirror ab = keyFor("a", "b");
    AnnotationMirror ba = keyFor("b", "a");
    AnnotationMirror abc = keyFor("a", "b", "c");
    AnnotationMirror bc = keyFor("b", "c");
    AnnotationMirror cb = keyFor("c", "b");
    AnnotationMirror cba = keyFor("c", "b", "a");
    AnnotationMirror dba = keyFor("d", "b", "a");

    // A new result has sorted values, regardless of the order of the arguments or their values.
    lubAssert(cba, dba, ab);
    lubAssert(dba, cba, ab);
    glbAssert(ba, cb, abc);
    glbAssert(cb, ba, abc);
    glbAssert(a, cb, abc);
    glbAssert(cb, a, abc);

    // If the result has the same values as an argument, the result is that argument.
    lubAssert(ba, ba, ba);
    glbAssert(ba, ba, ba);
    lubAssert(cba, ba, ba);
    lubAssert(ba, cba, ba);
    glbAssert(cba, cb, cba);
    glbAssert(cb, cba, cba);
    lubAssert(cba, bc, bc);
    glbAssert(cb, bc, cb);

    KeyForAnnotatedTypeFactory atypeFactory =
        (KeyForAnnotatedTypeFactory) ((BaseTypeVisitor<?>) visitor).getTypeFactory();
    QualifierHierarchy qualHierarchy = atypeFactory.getQualifierHierarchy();

    // The canonical form has sorted values without duplicates.
    AnnotationMirror bab = keyFor("b", "a", "b");
    sameAssert("canonicalize(" + ba + ")", qualHierarchy.canonicalize(ba), ab);
    sameAssert("canonicalize(" + bab + ")", qualHierarchy.canonicalize(bab), ab);
    sameAssert("canonicalAnnotation(" + ba + ")", atypeFactory.canonicalAnnotation(ba), ab);
    if (qualHierarchy.canonicalize(ab) != ab) {
      throw new AssertionError("canonicalize(" + ab + ") should return its argument");
    }
    sameAssert(
        "createKeyForAnnotationMirrorWithValue([b, a])",
        atypeFactory.createKeyForAnnotationMirrorWithValue(
            new LinkedHashSet<>(Arrays.asList("b", "a"))),
        ab);

    // For canonical arguments, the result does not depend on the order of the arguments or their
    // values.
    AnnotationMirror canonicalBa = qualHierarchy.canonicalize(ba);
    lubAssert(ab, canonicalBa, ab);
    lubAssert(canonicalBa, ab, ab);
    glbAssert(ab, canonicalBa, ab);
    glbAssert(canonicalBa, ab, ab);
  }
}
