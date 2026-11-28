package org.checkerframework.framework.util.typeinference8.types;

import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.testchecker.h1h2checker.H1H2AnnotatedTypeFactory;
import org.checkerframework.framework.testchecker.h1h2checker.H1H2Checker;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S2;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Top;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2Bot;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2S2;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2Top;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests of {@link IgnoredAnnotations#copyPrimaryAnnotations} for type variables, with two qualifier
 * hierarchies, H1 and H2. A type variable can have a primary annotation in one hierarchy and get
 * its qualifiers in the other hierarchy from its bounds; then the bounds of the target must be
 * those of the source in that other hierarchy.
 */
public class CopyPrimaryAnnotationsTest {

  /** Creates a new CopyPrimaryAnnotationsTest. */
  public CopyPrimaryAnnotationsTest() {}

  /**
   * The source has a primary annotation in H1 only, and the target has primary annotations in both
   * hierarchies. The target keeps no primary annotation in H2, and its bounds in H2 are those of
   * the source, so the lower bound of the target in H2 is not replaced by the upper bound's.
   */
  @Test
  public void primaryAnnotationInOneHierarchy() {
    AnnotatedTypeVariable source = typeVariable(0, List.of(h1s1), List.of(h2Top), List.of(h2Bot));
    AnnotatedTypeVariable target =
        typeVariable(0, List.of(h1s2, h2s1), List.of(h1Top, h2Top), List.of(h1Top, h2Top));

    IgnoredAnnotations.copyPrimaryAnnotations(source, target, qh);

    assertAnnotations("primary", List.of(h1s1), target.getPrimaryAnnotations());
    assertAnnotations(
        "upper bound", List.of(h1s1, h2Top), target.getUpperBound().getPrimaryAnnotations());
    assertAnnotations(
        "lower bound", List.of(h1s1, h2Bot), target.getLowerBound().getPrimaryAnnotations());
  }

  /**
   * The source has no primary annotation. The target loses its primary annotations, and its bounds
   * get the annotations of the source's bounds.
   */
  @Test
  public void noPrimaryAnnotation() {
    AnnotatedTypeVariable source =
        typeVariable(0, List.of(), List.of(h1Top, h2Top), List.of(h1s1, h2Bot));
    AnnotatedTypeVariable target =
        typeVariable(0, List.of(h1s2, h2s1), List.of(h1Top, h2Top), List.of(h1Top, h2Top));

    IgnoredAnnotations.copyPrimaryAnnotations(source, target, qh);

    assertAnnotations("primary", List.of(), target.getPrimaryAnnotations());
    assertAnnotations(
        "upper bound", List.of(h1Top, h2Top), target.getUpperBound().getPrimaryAnnotations());
    assertAnnotations(
        "lower bound", List.of(h1s1, h2Bot), target.getLowerBound().getPrimaryAnnotations());
  }

  /**
   * The upper bound of each type variable is another type variable. The source's has a primary
   * annotation in H1 only, and the target's has one in both hierarchies. In H2, the target's upper
   * bound loses its primary annotation, and its own bounds get those of the source's upper bound,
   * just as for the type variables themselves.
   */
  @Test
  public void typeVariableUpperBound() {
    AnnotatedTypeVariable sourceBound =
        typeVariable(1, List.of(h1s1), List.of(h2Top), List.of(h2Bot));
    AnnotatedTypeVariable source = typeVariable(0, sourceBound, List.of(h1Top, h2Bot));
    AnnotatedTypeVariable targetBound =
        typeVariable(1, List.of(h1s2, h2s2), List.of(h1Top, h2Top), List.of(h1Top, h2Top));
    AnnotatedTypeVariable target = typeVariable(0, targetBound, List.of(h1Top, h2Top));

    IgnoredAnnotations.copyPrimaryAnnotations(source, target, qh);

    AnnotatedTypeVariable newBound = (AnnotatedTypeVariable) target.getUpperBound();
    assertAnnotations("primary", List.of(), target.getPrimaryAnnotations());
    assertAnnotations("upper bound", List.of(h1s1), newBound.getPrimaryAnnotations());
    assertAnnotations(
        "upper bound's upper bound",
        List.of(h1s1, h2Top),
        newBound.getUpperBound().getPrimaryAnnotations());
    assertAnnotations(
        "upper bound's lower bound",
        List.of(h1s1, h2Bot),
        newBound.getLowerBound().getPrimaryAnnotations());
    assertAnnotations(
        "lower bound", List.of(h1Top, h2Bot), target.getLowerBound().getPrimaryAnnotations());
  }

  /** The processing environment of {@link #typeFactory}. */
  private static final ProcessingEnvironment env;

  /** The type factory of a checker with two qualifier hierarchies, H1 and H2. */
  private static final AnnotatedTypeFactory typeFactory;

  /** The qualifier hierarchy of {@link #typeFactory}. */
  private static final QualifierHierarchy qh;

  /** The qualifiers that the tests use. */
  private static final AnnotationMirror h1Top, h1s1, h1s2, h2Top, h2s1, h2s2, h2Bot;

  static {
    Context context = new Context();
    env = JavacProcessingEnvironment.instance(context);
    JavaCompiler javac = JavaCompiler.instance(context);
    // The list of modules must be initialized before entering symbols.
    javac.initModules(com.sun.tools.javac.util.List.nil());
    javac.enterDone();

    BaseTypeChecker checker = new H1H2Checker();
    checker.init(env);
    typeFactory = new H1H2AnnotatedTypeFactory(checker);
    qh = typeFactory.getQualifierHierarchy();

    Elements elements = env.getElementUtils();
    h1Top = AnnotationBuilder.fromClass(elements, H1Top.class);
    h1s1 = AnnotationBuilder.fromClass(elements, H1S1.class);
    h1s2 = AnnotationBuilder.fromClass(elements, H1S2.class);
    h2Top = AnnotationBuilder.fromClass(elements, H2Top.class);
    h2s1 = AnnotationBuilder.fromClass(elements, H2S1.class);
    h2s2 = AnnotationBuilder.fromClass(elements, H2S2.class);
    h2Bot = AnnotationBuilder.fromClass(elements, H2Bot.class);
  }

  /**
   * Returns a use of a type parameter of {@code java.util.Map}, whose bounds are {@code Object} and
   * the null type, with the given annotations. Adding a primary annotation also gives it to the
   * bounds, so {@code upper} and {@code lower} need only the annotations of the other hierarchies.
   *
   * @param index the index of a type parameter of {@code java.util.Map}: 0 for {@code K} or 1 for
   *     {@code V}
   * @param primary the primary annotations of the result
   * @param upper the annotations of the upper bound, before {@code primary} is added
   * @param lower the annotations of the lower bound, before {@code primary} is added
   * @return a use of a type parameter of {@code java.util.Map}
   */
  private static AnnotatedTypeVariable typeVariable(
      int index,
      List<AnnotationMirror> primary,
      List<AnnotationMirror> upper,
      List<AnnotationMirror> lower) {
    AnnotatedTypeMirror upperBound =
        AnnotatedTypeMirror.createType(
            env.getElementUtils().getTypeElement("java.lang.Object").asType(), typeFactory, false);
    upperBound.addAnnotations(upper);
    AnnotatedTypeVariable result = typeVariable(index, upperBound, lower);
    result.addAnnotations(primary);
    return result;
  }

  /**
   * Returns a use of a type parameter of {@code java.util.Map}, with no primary annotation, whose
   * upper bound is {@code upperBound} and whose lower bound is the null type.
   *
   * @param index the index of a type parameter of {@code java.util.Map}: 0 for {@code K} or 1 for
   *     {@code V}
   * @param upperBound the upper bound of the result
   * @param lower the annotations of the lower bound
   * @return a use of a type parameter of {@code java.util.Map}
   */
  private static AnnotatedTypeVariable typeVariable(
      int index, AnnotatedTypeMirror upperBound, List<AnnotationMirror> lower) {
    TypeParameterElement parameter =
        env.getElementUtils().getTypeElement("java.util.Map").getTypeParameters().get(index);
    AnnotatedTypeVariable result =
        (AnnotatedTypeVariable)
            AnnotatedTypeMirror.createType(parameter.asType(), typeFactory, false);
    TypeMirror nullType = env.getTypeUtils().getNullType();
    AnnotatedTypeMirror lowerBound = AnnotatedTypeMirror.createType(nullType, typeFactory, false);
    lowerBound.addAnnotations(lower);
    result.setUpperBound(upperBound);
    result.setLowerBound(lowerBound);
    return result;
  }

  /**
   * Asserts that {@code actual} contains the same annotations as {@code expected}.
   *
   * @param what what the annotations are on; used for diagnostics
   * @param expected the expected annotations
   * @param actual the actual annotations
   */
  private static void assertAnnotations(
      String what, List<AnnotationMirror> expected, Set<AnnotationMirror> actual) {
    Assert.assertTrue(
        what + ": expected " + expected + " but was " + actual,
        AnnotationUtils.areSame(expected, Arrays.asList(actual.toArray(new AnnotationMirror[0]))));
  }
}
