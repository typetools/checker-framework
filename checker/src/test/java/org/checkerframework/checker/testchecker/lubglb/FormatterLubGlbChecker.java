package org.checkerframework.checker.testchecker.lubglb;

// Test case for issues 691 and 756.
// https://github.com/typetools/checker-framework/issues/691
// https://github.com/typetools/checker-framework/issues/756

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.formatter.FormatterAnnotatedTypeFactory;
import org.checkerframework.checker.formatter.FormatterChecker;
import org.checkerframework.checker.formatter.FormatterTreeUtil;
import org.checkerframework.checker.formatter.FormatterVisitor;
import org.checkerframework.checker.formatter.qual.ConversionCategory;
import org.checkerframework.checker.formatter.qual.Format;
import org.checkerframework.checker.formatter.qual.FormatBottom;
import org.checkerframework.checker.formatter.qual.InvalidFormat;
import org.checkerframework.checker.formatter.qual.UnknownFormat;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * This class tests the implementation of GLB computation in the Formatter Checker, but it does not
 * test for the crash described in issue 691. That is done by tests/all-systems/Issue691.java. It
 * also tests the implementation of LUB computation in the Formatter Checker.
 */
public class FormatterLubGlbChecker extends FormatterChecker {

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new FormatterVisitor(this) {
      @Override
      protected FormatterLubGlbAnnotatedTypeFactory createTypeFactory() {
        return new FormatterLubGlbAnnotatedTypeFactory(checker);
      }
    };
  }

  /** FormatterLubGlbAnnotatedTypeFactory. */
  private static class FormatterLubGlbAnnotatedTypeFactory extends FormatterAnnotatedTypeFactory {

    /**
     * Constructor.
     *
     * @param checker checker
     */
    public FormatterLubGlbAnnotatedTypeFactory(BaseTypeChecker checker) {
      super(checker);
      postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
      return new HashSet<>(
          Arrays.asList(
              FormatBottom.class, Format.class, InvalidFormat.class, UnknownFormat.class));
    }
  }

  /**
   * Throws an exception if glb(arg1, arg2) != result.
   *
   * @param arg1 the first argument
   * @param arg2 the second argument
   * @param expected the expected result
   */
  private void glbAssert(AnnotationMirror arg1, AnnotationMirror arg2, AnnotationMirror expected) {
    QualifierHierarchy qualHierarchy =
        ((BaseTypeVisitor<?>) visitor).getTypeFactory().getQualifierHierarchy();
    AnnotationMirror result = qualHierarchy.greatestLowerBound(arg1, arg2);
    if (!AnnotationUtils.areSame(expected, result)) {
      throw new AssertionError(
          String.format("GLB of %s and %s should be %s, but is %s", arg1, arg2, expected, result));
    }
  }

  /**
   * Throws an exception if lub(arg1, arg2) != result.
   *
   * @param arg1 the first argument
   * @param arg2 the second argument
   * @param expected the expected result
   */
  private void lubAssert(AnnotationMirror arg1, AnnotationMirror arg2, AnnotationMirror expected) {
    QualifierHierarchy qualHierarchy =
        ((BaseTypeVisitor<?>) visitor).getTypeFactory().getQualifierHierarchy();
    AnnotationMirror result = qualHierarchy.leastUpperBound(arg1, arg2);
    if (!AnnotationUtils.areSame(expected, result)) {
      throw new AssertionError(
          String.format("LUB of %s and %s should be %s, but is %s", arg1, arg2, expected, result));
    }
  }

  @SuppressWarnings("checkstyle:localvariablename")
  @Override
  public void initChecker() {
    super.initChecker();
    FormatterTreeUtil treeUtil = new FormatterTreeUtil(this);

    Elements elements = getElementUtils();
    AnnotationMirror UNKNOWNFORMAT = AnnotationBuilder.fromClass(elements, UnknownFormat.class);
    AnnotationMirror FORMAT =
        AnnotationBuilder.fromClass(
            elements,
            Format.class,
            AnnotationBuilder.elementNamesValues("value", new ConversionCategory[0]));
    AnnotationMirror INVALIDFORMAT =
        AnnotationBuilder.fromClass(
            elements, InvalidFormat.class, AnnotationBuilder.elementNamesValues("value", "dummy"));
    AnnotationMirror FORMATBOTTOM = AnnotationBuilder.fromClass(elements, FormatBottom.class);

    AnnotationBuilder builder = new AnnotationBuilder(processingEnv, InvalidFormat.class);
    builder.setValue("value", "Message");
    AnnotationMirror invalidFormatWithMessage = builder.build();

    builder = new AnnotationBuilder(processingEnv, InvalidFormat.class);
    builder.setValue("value", "Message2");
    AnnotationMirror invalidFormatWithMessage2 = builder.build();

    builder = new AnnotationBuilder(processingEnv, InvalidFormat.class);
    builder.setValue("value", "(\"Message\" or \"Message2\")");
    AnnotationMirror invalidFormatWithMessagesOred = builder.build();

    builder = new AnnotationBuilder(processingEnv, InvalidFormat.class);
    builder.setValue("value", "(\"Message\" and \"Message2\")");
    AnnotationMirror invalidFormatWithMessagesAnded = builder.build();

    ConversionCategory[] cc = new ConversionCategory[1];

    cc[0] = ConversionCategory.UNUSED;
    AnnotationMirror formatUnusedAnno = treeUtil.categoriesToFormatAnnotation(cc);
    cc[0] = ConversionCategory.GENERAL;
    AnnotationMirror formatGeneralAnno = treeUtil.categoriesToFormatAnnotation(cc);
    cc[0] = ConversionCategory.CHAR;
    AnnotationMirror formatCharAnno = treeUtil.categoriesToFormatAnnotation(cc);
    cc[0] = ConversionCategory.INT;
    AnnotationMirror formatIntAnno = treeUtil.categoriesToFormatAnnotation(cc);
    cc[0] = ConversionCategory.TIME;
    AnnotationMirror formatTimeAnno = treeUtil.categoriesToFormatAnnotation(cc);
    cc[0] = ConversionCategory.FLOAT;
    AnnotationMirror formatFloatAnno = treeUtil.categoriesToFormatAnnotation(cc);
    cc[0] = ConversionCategory.CHAR_AND_INT;
    AnnotationMirror formatCharAndIntAnno = treeUtil.categoriesToFormatAnnotation(cc);
    cc[0] = ConversionCategory.INT_AND_TIME;
    AnnotationMirror formatIntAndTimeAnno = treeUtil.categoriesToFormatAnnotation(cc);
    cc[0] = ConversionCategory.NULL;
    AnnotationMirror formatNullAnno = treeUtil.categoriesToFormatAnnotation(cc);

    // ** GLB tests **

    glbAssert(formatCharAndIntAnno, formatIntAndTimeAnno, formatIntAnno);

    // GLB of UNUSED and others

    glbAssert(formatUnusedAnno, formatUnusedAnno, formatUnusedAnno);
    glbAssert(formatUnusedAnno, formatGeneralAnno, formatUnusedAnno);
    glbAssert(formatUnusedAnno, formatCharAnno, formatUnusedAnno);
    glbAssert(formatUnusedAnno, formatIntAnno, formatUnusedAnno);
    glbAssert(formatUnusedAnno, formatTimeAnno, formatUnusedAnno);
    glbAssert(formatUnusedAnno, formatFloatAnno, formatUnusedAnno);
    glbAssert(formatUnusedAnno, formatCharAndIntAnno, formatUnusedAnno);
    glbAssert(formatUnusedAnno, formatIntAndTimeAnno, formatUnusedAnno);
    glbAssert(formatUnusedAnno, formatNullAnno, formatUnusedAnno);

    // GLB of GENERAL and others

    glbAssert(formatGeneralAnno, formatUnusedAnno, formatUnusedAnno);
    glbAssert(formatGeneralAnno, formatGeneralAnno, formatGeneralAnno);
    glbAssert(formatGeneralAnno, formatCharAnno, formatGeneralAnno);
    glbAssert(formatGeneralAnno, formatIntAnno, formatGeneralAnno);
    glbAssert(formatGeneralAnno, formatTimeAnno, formatGeneralAnno);
    glbAssert(formatGeneralAnno, formatFloatAnno, formatGeneralAnno);
    glbAssert(formatGeneralAnno, formatCharAndIntAnno, formatGeneralAnno);
    glbAssert(formatGeneralAnno, formatIntAndTimeAnno, formatGeneralAnno);
    glbAssert(formatGeneralAnno, formatNullAnno, formatGeneralAnno);

    // GLB of CHAR and others

    glbAssert(formatCharAnno, formatUnusedAnno, formatUnusedAnno);
    glbAssert(formatCharAnno, formatGeneralAnno, formatGeneralAnno);
    glbAssert(formatCharAnno, formatCharAnno, formatCharAnno);
    glbAssert(formatCharAnno, formatIntAnno, formatGeneralAnno);
    glbAssert(formatCharAnno, formatTimeAnno, formatGeneralAnno);
    glbAssert(formatCharAnno, formatFloatAnno, formatGeneralAnno);
    glbAssert(formatCharAnno, formatCharAndIntAnno, formatCharAnno);
    glbAssert(formatCharAnno, formatIntAndTimeAnno, formatGeneralAnno);
    glbAssert(formatCharAnno, formatNullAnno, formatCharAnno);

    // GLB of INT and others

    glbAssert(formatIntAnno, formatUnusedAnno, formatUnusedAnno);
    glbAssert(formatIntAnno, formatGeneralAnno, formatGeneralAnno);
    glbAssert(formatIntAnno, formatCharAnno, formatGeneralAnno);
    glbAssert(formatIntAnno, formatIntAnno, formatIntAnno);
    glbAssert(formatIntAnno, formatTimeAnno, formatGeneralAnno);
    glbAssert(formatIntAnno, formatFloatAnno, formatGeneralAnno);
    glbAssert(formatIntAnno, formatCharAndIntAnno, formatIntAnno);
    glbAssert(formatIntAnno, formatIntAndTimeAnno, formatIntAnno);
    glbAssert(formatIntAnno, formatNullAnno, formatIntAnno);

    // GLB of TIME and others

    glbAssert(formatTimeAnno, formatUnusedAnno, formatUnusedAnno);
    glbAssert(formatTimeAnno, formatGeneralAnno, formatGeneralAnno);
    glbAssert(formatTimeAnno, formatCharAnno, formatGeneralAnno);
    glbAssert(formatTimeAnno, formatIntAnno, formatGeneralAnno);
    glbAssert(formatTimeAnno, formatTimeAnno, formatTimeAnno);
    glbAssert(formatTimeAnno, formatFloatAnno, formatGeneralAnno);
    glbAssert(formatTimeAnno, formatCharAndIntAnno, formatGeneralAnno);
    glbAssert(formatTimeAnno, formatIntAndTimeAnno, formatTimeAnno);
    glbAssert(formatTimeAnno, formatNullAnno, formatTimeAnno);

    // GLB of FLOAT and others

    glbAssert(formatFloatAnno, formatUnusedAnno, formatUnusedAnno);
    glbAssert(formatFloatAnno, formatGeneralAnno, formatGeneralAnno);
    glbAssert(formatFloatAnno, formatCharAnno, formatGeneralAnno);
    glbAssert(formatFloatAnno, formatIntAnno, formatGeneralAnno);
    glbAssert(formatFloatAnno, formatTimeAnno, formatGeneralAnno);
    glbAssert(formatFloatAnno, formatFloatAnno, formatFloatAnno);
    glbAssert(formatFloatAnno, formatCharAndIntAnno, formatGeneralAnno);
    glbAssert(formatFloatAnno, formatIntAndTimeAnno, formatGeneralAnno);
    glbAssert(formatFloatAnno, formatNullAnno, formatFloatAnno);

    // GLB of CHAR_AND_INT and others

    glbAssert(formatCharAndIntAnno, formatUnusedAnno, formatUnusedAnno);
    glbAssert(formatCharAndIntAnno, formatGeneralAnno, formatGeneralAnno);
    glbAssert(formatCharAndIntAnno, formatCharAnno, formatCharAnno);
    glbAssert(formatCharAndIntAnno, formatIntAnno, formatIntAnno);
    glbAssert(formatCharAndIntAnno, formatTimeAnno, formatGeneralAnno);
    glbAssert(formatCharAndIntAnno, formatFloatAnno, formatGeneralAnno);
    glbAssert(formatCharAndIntAnno, formatCharAndIntAnno, formatCharAndIntAnno);
    glbAssert(formatCharAndIntAnno, formatIntAndTimeAnno, formatIntAnno);
    glbAssert(formatCharAndIntAnno, formatNullAnno, formatCharAndIntAnno);

    // GLB of INT_AND_TIME and others

    glbAssert(formatIntAndTimeAnno, formatUnusedAnno, formatUnusedAnno);
    glbAssert(formatIntAndTimeAnno, formatGeneralAnno, formatGeneralAnno);
    glbAssert(formatIntAndTimeAnno, formatCharAnno, formatGeneralAnno);
    glbAssert(formatIntAndTimeAnno, formatIntAnno, formatIntAnno);
    glbAssert(formatIntAndTimeAnno, formatTimeAnno, formatTimeAnno);
    glbAssert(formatIntAndTimeAnno, formatFloatAnno, formatGeneralAnno);
    glbAssert(formatIntAndTimeAnno, formatCharAndIntAnno, formatIntAnno);
    glbAssert(formatIntAndTimeAnno, formatIntAndTimeAnno, formatIntAndTimeAnno);
    glbAssert(formatIntAndTimeAnno, formatNullAnno, formatIntAndTimeAnno);

    // GLB of NULL and others

    glbAssert(formatNullAnno, formatUnusedAnno, formatUnusedAnno);
    glbAssert(formatNullAnno, formatGeneralAnno, formatGeneralAnno);
    glbAssert(formatNullAnno, formatCharAnno, formatCharAnno);
    glbAssert(formatNullAnno, formatIntAnno, formatIntAnno);
    glbAssert(formatNullAnno, formatTimeAnno, formatTimeAnno);
    glbAssert(formatNullAnno, formatFloatAnno, formatFloatAnno);
    glbAssert(formatNullAnno, formatCharAndIntAnno, formatCharAndIntAnno);
    glbAssert(formatNullAnno, formatIntAndTimeAnno, formatIntAndTimeAnno);
    glbAssert(formatNullAnno, formatNullAnno, formatNullAnno);

    // Now test with two ConversionCategory at a time:

    ConversionCategory[] cc2 = new ConversionCategory[2];

    cc2[0] = ConversionCategory.CHAR_AND_INT;
    cc2[1] = ConversionCategory.FLOAT;
    AnnotationMirror formatTwoConvCat1 = treeUtil.categoriesToFormatAnnotation(cc2);
    cc2[0] = ConversionCategory.INT;
    cc2[1] = ConversionCategory.CHAR;
    AnnotationMirror formatTwoConvCat2 = treeUtil.categoriesToFormatAnnotation(cc2);
    cc2[0] = ConversionCategory.INT;
    cc2[1] = ConversionCategory.GENERAL;
    AnnotationMirror formatTwoConvCat3 = treeUtil.categoriesToFormatAnnotation(cc2);

    glbAssert(formatTwoConvCat1, formatTwoConvCat2, formatTwoConvCat3);

    // Test that the GLB of two ConversionCategory arrays of different sizes is an array of the
    // smallest size of the two:

    glbAssert(formatGeneralAnno, formatTwoConvCat1, formatGeneralAnno);
    glbAssert(formatTwoConvCat2, formatNullAnno, formatIntAnno);

    // GLB of @UnknownFormat and others

    glbAssert(UNKNOWNFORMAT, UNKNOWNFORMAT, UNKNOWNFORMAT);
    glbAssert(UNKNOWNFORMAT, FORMAT, FORMAT);
    glbAssert(UNKNOWNFORMAT, formatUnusedAnno, formatUnusedAnno);
    glbAssert(UNKNOWNFORMAT, INVALIDFORMAT, INVALIDFORMAT);
    glbAssert(UNKNOWNFORMAT, invalidFormatWithMessage, invalidFormatWithMessage);
    glbAssert(UNKNOWNFORMAT, FORMATBOTTOM, FORMATBOTTOM);

    // GLB of @Format(null) and others

    glbAssert(FORMAT, UNKNOWNFORMAT, FORMAT);
    // Computing the GLB of @Format(null) and @Format(null) should never occur in practice;
    // skipping this case as it causes an expected crash.
    // Computing the GLB of @Format(null) and @Format with a value should never occur in
    // practice; skipping this case as it causes an expected crash.
    glbAssert(FORMAT, INVALIDFORMAT, FORMATBOTTOM);
    glbAssert(FORMAT, invalidFormatWithMessage, FORMATBOTTOM);
    glbAssert(FORMAT, FORMATBOTTOM, FORMATBOTTOM);

    // GLB of @Format(UNUSED) and others

    glbAssert(formatUnusedAnno, UNKNOWNFORMAT, formatUnusedAnno);
    // Computing the GLB of @Format with a value and @Format(null) should never occur in
    // practice; skipping this case as it causes an expected crash.
    glbAssert(formatUnusedAnno, formatUnusedAnno, formatUnusedAnno);
    glbAssert(formatUnusedAnno, INVALIDFORMAT, FORMATBOTTOM);
    glbAssert(formatUnusedAnno, invalidFormatWithMessage, FORMATBOTTOM);
    glbAssert(formatUnusedAnno, FORMATBOTTOM, FORMATBOTTOM);

    // GLB of @InvalidFormat(null) and others

    glbAssert(INVALIDFORMAT, UNKNOWNFORMAT, INVALIDFORMAT);
    glbAssert(INVALIDFORMAT, FORMAT, FORMATBOTTOM);
    glbAssert(INVALIDFORMAT, formatUnusedAnno, FORMATBOTTOM);
    glbAssert(INVALIDFORMAT, FORMATBOTTOM, FORMATBOTTOM);

    // GLB of @InvalidFormat("Message") and others

    glbAssert(invalidFormatWithMessage, UNKNOWNFORMAT, invalidFormatWithMessage);
    glbAssert(invalidFormatWithMessage, FORMAT, FORMATBOTTOM);
    glbAssert(invalidFormatWithMessage, formatUnusedAnno, FORMATBOTTOM);
    glbAssert(invalidFormatWithMessage, invalidFormatWithMessage, invalidFormatWithMessage);
    glbAssert(invalidFormatWithMessage, invalidFormatWithMessage2, invalidFormatWithMessagesAnded);
    glbAssert(invalidFormatWithMessage, FORMATBOTTOM, FORMATBOTTOM);

    // GLB of @FormatBottom and others

    glbAssert(FORMATBOTTOM, UNKNOWNFORMAT, FORMATBOTTOM);
    glbAssert(FORMATBOTTOM, FORMAT, FORMATBOTTOM);
    glbAssert(FORMATBOTTOM, formatUnusedAnno, FORMATBOTTOM);
    glbAssert(FORMATBOTTOM, INVALIDFORMAT, FORMATBOTTOM);
    glbAssert(FORMATBOTTOM, invalidFormatWithMessage, FORMATBOTTOM);
    glbAssert(FORMATBOTTOM, FORMATBOTTOM, FORMATBOTTOM);

    // ** LUB tests **

    // LUB of UNUSED and others

    lubAssert(formatUnusedAnno, formatUnusedAnno, formatUnusedAnno);
    lubAssert(formatUnusedAnno, formatGeneralAnno, formatGeneralAnno);
    lubAssert(formatUnusedAnno, formatCharAnno, formatCharAnno);
    lubAssert(formatUnusedAnno, formatIntAnno, formatIntAnno);
    lubAssert(formatUnusedAnno, formatTimeAnno, formatTimeAnno);
    lubAssert(formatUnusedAnno, formatFloatAnno, formatFloatAnno);
    lubAssert(formatUnusedAnno, formatCharAndIntAnno, formatCharAndIntAnno);
    lubAssert(formatUnusedAnno, formatIntAndTimeAnno, formatIntAndTimeAnno);
    lubAssert(formatUnusedAnno, formatNullAnno, formatNullAnno);

    // LUB of GENERAL and others

    lubAssert(formatGeneralAnno, formatUnusedAnno, formatGeneralAnno);
    lubAssert(formatGeneralAnno, formatGeneralAnno, formatGeneralAnno);
    lubAssert(formatGeneralAnno, formatCharAnno, formatCharAnno);
    lubAssert(formatGeneralAnno, formatIntAnno, formatIntAnno);
    lubAssert(formatGeneralAnno, formatTimeAnno, formatTimeAnno);
    lubAssert(formatGeneralAnno, formatFloatAnno, formatFloatAnno);
    lubAssert(formatGeneralAnno, formatCharAndIntAnno, formatCharAndIntAnno);
    lubAssert(formatGeneralAnno, formatIntAndTimeAnno, formatIntAndTimeAnno);
    lubAssert(formatGeneralAnno, formatNullAnno, formatNullAnno);

    // LUB of CHAR and others

    lubAssert(formatCharAnno, formatUnusedAnno, formatCharAnno);
    lubAssert(formatCharAnno, formatGeneralAnno, formatCharAnno);
    lubAssert(formatCharAnno, formatCharAnno, formatCharAnno);
    lubAssert(formatCharAnno, formatIntAnno, formatCharAndIntAnno);
    lubAssert(formatCharAnno, formatTimeAnno, formatNullAnno);
    lubAssert(formatCharAnno, formatFloatAnno, formatNullAnno);
    lubAssert(formatCharAnno, formatCharAndIntAnno, formatCharAndIntAnno);
    lubAssert(formatCharAnno, formatIntAndTimeAnno, formatNullAnno);
    lubAssert(formatCharAnno, formatNullAnno, formatNullAnno);

    // LUB of INT and others

    lubAssert(formatIntAnno, formatUnusedAnno, formatIntAnno);
    lubAssert(formatIntAnno, formatGeneralAnno, formatIntAnno);
    lubAssert(formatIntAnno, formatCharAnno, formatCharAndIntAnno);
    lubAssert(formatIntAnno, formatIntAnno, formatIntAnno);
    lubAssert(formatIntAnno, formatTimeAnno, formatIntAndTimeAnno);
    lubAssert(formatIntAnno, formatFloatAnno, formatNullAnno);
    lubAssert(formatIntAnno, formatCharAndIntAnno, formatCharAndIntAnno);
    lubAssert(formatIntAnno, formatIntAndTimeAnno, formatIntAndTimeAnno);
    lubAssert(formatIntAnno, formatNullAnno, formatNullAnno);

    // LUB of TIME and others

    lubAssert(formatTimeAnno, formatUnusedAnno, formatTimeAnno);
    lubAssert(formatTimeAnno, formatGeneralAnno, formatTimeAnno);
    lubAssert(formatTimeAnno, formatCharAnno, formatNullAnno);
    lubAssert(formatTimeAnno, formatIntAnno, formatIntAndTimeAnno);
    lubAssert(formatTimeAnno, formatTimeAnno, formatTimeAnno);
    lubAssert(formatTimeAnno, formatFloatAnno, formatNullAnno);
    lubAssert(formatTimeAnno, formatCharAndIntAnno, formatNullAnno);
    lubAssert(formatTimeAnno, formatIntAndTimeAnno, formatIntAndTimeAnno);
    lubAssert(formatTimeAnno, formatNullAnno, formatNullAnno);

    // LUB of FLOAT and others

    lubAssert(formatFloatAnno, formatUnusedAnno, formatFloatAnno);
    lubAssert(formatFloatAnno, formatGeneralAnno, formatFloatAnno);
    lubAssert(formatFloatAnno, formatCharAnno, formatNullAnno);
    lubAssert(formatFloatAnno, formatIntAnno, formatNullAnno);
    lubAssert(formatFloatAnno, formatTimeAnno, formatNullAnno);
    lubAssert(formatFloatAnno, formatFloatAnno, formatFloatAnno);
    lubAssert(formatFloatAnno, formatCharAndIntAnno, formatNullAnno);
    lubAssert(formatFloatAnno, formatIntAndTimeAnno, formatNullAnno);
    lubAssert(formatFloatAnno, formatNullAnno, formatNullAnno);

    // LUB of CHAR_AND_INT and others

    lubAssert(formatCharAndIntAnno, formatUnusedAnno, formatCharAndIntAnno);
    lubAssert(formatCharAndIntAnno, formatGeneralAnno, formatCharAndIntAnno);
    lubAssert(formatCharAndIntAnno, formatCharAnno, formatCharAndIntAnno);
    lubAssert(formatCharAndIntAnno, formatIntAnno, formatCharAndIntAnno);
    lubAssert(formatCharAndIntAnno, formatTimeAnno, formatNullAnno);
    lubAssert(formatCharAndIntAnno, formatFloatAnno, formatNullAnno);
    lubAssert(formatCharAndIntAnno, formatCharAndIntAnno, formatCharAndIntAnno);
    lubAssert(formatCharAndIntAnno, formatIntAndTimeAnno, formatNullAnno);
    lubAssert(formatCharAndIntAnno, formatNullAnno, formatNullAnno);

    // LUB of INT_AND_TIME and others

    lubAssert(formatIntAndTimeAnno, formatUnusedAnno, formatIntAndTimeAnno);
    lubAssert(formatIntAndTimeAnno, formatGeneralAnno, formatIntAndTimeAnno);
    lubAssert(formatIntAndTimeAnno, formatCharAnno, formatNullAnno);
    lubAssert(formatIntAndTimeAnno, formatIntAnno, formatIntAndTimeAnno);
    lubAssert(formatIntAndTimeAnno, formatTimeAnno, formatIntAndTimeAnno);
    lubAssert(formatIntAndTimeAnno, formatFloatAnno, formatNullAnno);
    lubAssert(formatIntAndTimeAnno, formatCharAndIntAnno, formatNullAnno);
    lubAssert(formatIntAndTimeAnno, formatIntAndTimeAnno, formatIntAndTimeAnno);
    lubAssert(formatIntAndTimeAnno, formatNullAnno, formatNullAnno);

    // LUB of NULL and others

    lubAssert(formatNullAnno, formatUnusedAnno, formatNullAnno);
    lubAssert(formatNullAnno, formatGeneralAnno, formatNullAnno);
    lubAssert(formatNullAnno, formatCharAnno, formatNullAnno);
    lubAssert(formatNullAnno, formatIntAnno, formatNullAnno);
    lubAssert(formatNullAnno, formatTimeAnno, formatNullAnno);
    lubAssert(formatNullAnno, formatFloatAnno, formatNullAnno);
    lubAssert(formatNullAnno, formatCharAndIntAnno, formatNullAnno);
    lubAssert(formatNullAnno, formatIntAndTimeAnno, formatNullAnno);
    lubAssert(formatNullAnno, formatNullAnno, formatNullAnno);

    // Now test with two ConversionCategory at a time:

    cc2[0] = ConversionCategory.CHAR_AND_INT;
    cc2[1] = ConversionCategory.NULL;
    AnnotationMirror formatTwoConvCat4 = treeUtil.categoriesToFormatAnnotation(cc2);
    cc2[0] = ConversionCategory.NULL;
    cc2[1] = ConversionCategory.CHAR;
    AnnotationMirror formatTwoConvCat5 = treeUtil.categoriesToFormatAnnotation(cc2);

    lubAssert(formatTwoConvCat1, formatTwoConvCat2, formatTwoConvCat4);

    // Test that the LUB of two ConversionCategory arrays of different sizes is an array of the
    // largest size of the two:

    lubAssert(formatGeneralAnno, formatTwoConvCat1, formatTwoConvCat1);
    lubAssert(formatTwoConvCat2, formatNullAnno, formatTwoConvCat5);

    // LUB of @UnknownFormat and others

    lubAssert(UNKNOWNFORMAT, UNKNOWNFORMAT, UNKNOWNFORMAT);
    lubAssert(UNKNOWNFORMAT, FORMAT, UNKNOWNFORMAT);
    lubAssert(UNKNOWNFORMAT, formatUnusedAnno, UNKNOWNFORMAT);
    lubAssert(UNKNOWNFORMAT, INVALIDFORMAT, UNKNOWNFORMAT);
    lubAssert(UNKNOWNFORMAT, invalidFormatWithMessage, UNKNOWNFORMAT);
    lubAssert(UNKNOWNFORMAT, FORMATBOTTOM, UNKNOWNFORMAT);

    // LUB of @Format(null) and others

    lubAssert(FORMAT, UNKNOWNFORMAT, UNKNOWNFORMAT);
    // Computing the LUB of @Format(null) and @Format(null) should never occur in practice;
    // skipping this case as it causes an expected crash.
    // Computing the LUB of @Format(null) and @Format with a value should never occur in
    // practice; skipping this case as it causes an expected crash.
    lubAssert(FORMAT, INVALIDFORMAT, UNKNOWNFORMAT);
    lubAssert(FORMAT, invalidFormatWithMessage, UNKNOWNFORMAT);
    lubAssert(FORMAT, FORMATBOTTOM, FORMAT);

    // LUB of @Format(UNUSED) and others

    lubAssert(formatUnusedAnno, UNKNOWNFORMAT, UNKNOWNFORMAT);
    // Computing the LUB of @Format with a value and @Format(null) should never occur in
    // practice; skipping this case as it causes an expected crash.
    lubAssert(formatUnusedAnno, formatUnusedAnno, formatUnusedAnno);
    lubAssert(formatUnusedAnno, INVALIDFORMAT, UNKNOWNFORMAT);
    lubAssert(formatUnusedAnno, invalidFormatWithMessage, UNKNOWNFORMAT);
    lubAssert(formatUnusedAnno, FORMATBOTTOM, formatUnusedAnno);

    // LUB of @InvalidFormat(null) and others

    lubAssert(INVALIDFORMAT, UNKNOWNFORMAT, UNKNOWNFORMAT);
    lubAssert(INVALIDFORMAT, FORMAT, UNKNOWNFORMAT);
    lubAssert(INVALIDFORMAT, formatUnusedAnno, UNKNOWNFORMAT);
    lubAssert(INVALIDFORMAT, FORMATBOTTOM, INVALIDFORMAT);

    // LUB of @InvalidFormat("Message") and others

    lubAssert(invalidFormatWithMessage, UNKNOWNFORMAT, UNKNOWNFORMAT);
    lubAssert(invalidFormatWithMessage, FORMAT, UNKNOWNFORMAT);
    lubAssert(invalidFormatWithMessage, formatUnusedAnno, UNKNOWNFORMAT);
    lubAssert(invalidFormatWithMessage, invalidFormatWithMessage, invalidFormatWithMessage);
    lubAssert(invalidFormatWithMessage, invalidFormatWithMessage2, invalidFormatWithMessagesOred);
    lubAssert(invalidFormatWithMessage, FORMATBOTTOM, invalidFormatWithMessage);

    // LUB of @FormatBottom and others

    lubAssert(FORMATBOTTOM, UNKNOWNFORMAT, UNKNOWNFORMAT);
    lubAssert(FORMATBOTTOM, FORMAT, FORMAT);
    lubAssert(FORMATBOTTOM, formatUnusedAnno, formatUnusedAnno);
    lubAssert(FORMATBOTTOM, INVALIDFORMAT, INVALIDFORMAT);
    lubAssert(FORMATBOTTOM, invalidFormatWithMessage, invalidFormatWithMessage);
    lubAssert(FORMATBOTTOM, FORMATBOTTOM, FORMATBOTTOM);
  }
}
