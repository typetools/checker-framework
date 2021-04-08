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

    QualifierHierarchy qh = ((BaseTypeVisitor<?>) visitor).getTypeFactory().getQualifierHierarchy();

    // ** GLB tests **

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatCharAndIntAnno, formatIntAndTimeAnno), formatIntAnno)
        : "GLB of @Format(CHAR_AND_INT) and @Format(INT_AND_TIME) is not @Format(INT)!";

    // GLB of UNUSED and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatUnusedAnno, formatUnusedAnno), formatUnusedAnno)
        : "GLB of @Format(UNUSED) and @Format(UNUSED) is not @Format(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatUnusedAnno, formatGeneralAnno), formatUnusedAnno)
        : "GLB of @Format(UNUSED) and @Format(GENERAL) is not @Format(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatUnusedAnno, formatCharAnno), formatUnusedAnno)
        : "GLB of @Format(UNUSED) and @Format(CHAR) is not @Format(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatUnusedAnno, formatIntAnno), formatUnusedAnno)
        : "GLB of @Format(UNUSED) and @Format(INT) is not @Format(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatUnusedAnno, formatTimeAnno), formatUnusedAnno)
        : "GLB of @Format(UNUSED) and @Format(TIME) is not @Format(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatUnusedAnno, formatFloatAnno), formatUnusedAnno)
        : "GLB of @Format(UNUSED) and @Format(FLOAT) is not @Format(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatUnusedAnno, formatCharAndIntAnno), formatUnusedAnno)
        : "GLB of @Format(UNUSED) and @Format(CHAR_AND_INT) is not @Format(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatUnusedAnno, formatIntAndTimeAnno), formatUnusedAnno)
        : "GLB of @Format(UNUSED) and @Format(INT_AND_TIME) is not @Format(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatUnusedAnno, formatNullAnno), formatUnusedAnno)
        : "GLB of @Format(UNUSED) and @Format(NULL) is not @Format(UNUSED)!";

    // GLB of GENERAL and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatGeneralAnno, formatUnusedAnno), formatUnusedAnno)
        : "GLB of @Format(GENERAL) and @Format(UNUSED) is not @Format(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatGeneralAnno, formatGeneralAnno), formatGeneralAnno)
        : "GLB of @Format(GENERAL) and @Format(GENERAL) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatGeneralAnno, formatCharAnno), formatGeneralAnno)
        : "GLB of @Format(GENERAL) and @Format(CHAR) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatGeneralAnno, formatIntAnno), formatGeneralAnno)
        : "GLB of @Format(GENERAL) and @Format(INT) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatGeneralAnno, formatTimeAnno), formatGeneralAnno)
        : "GLB of @Format(GENERAL) and @Format(TIME) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatGeneralAnno, formatFloatAnno), formatGeneralAnno)
        : "GLB of @Format(GENERAL) and @Format(FLOAT) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatGeneralAnno, formatCharAndIntAnno), formatGeneralAnno)
        : "GLB of @Format(GENERAL) and @Format(CHAR_AND_INT) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatGeneralAnno, formatIntAndTimeAnno), formatGeneralAnno)
        : "GLB of @Format(GENERAL) and @Format(INT_AND_TIME) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatGeneralAnno, formatNullAnno), formatGeneralAnno)
        : "GLB of @Format(GENERAL) and @Format(NULL) is not @Format(GENERAL)!";

    // GLB of CHAR and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatCharAnno, formatUnusedAnno), formatUnusedAnno)
        : "GLB of @Format(CHAR) and @Format(UNUSED) is not @Format(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatCharAnno, formatGeneralAnno), formatGeneralAnno)
        : "GLB of @Format(CHAR) and @Format(GENERAL) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatCharAnno, formatCharAnno), formatCharAnno)
        : "GLB of @Format(CHAR) and @Format(CHAR) is not @Format(CHAR)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatCharAnno, formatIntAnno), formatGeneralAnno)
        : "GLB of @Format(CHAR) and @Format(INT) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatCharAnno, formatTimeAnno), formatGeneralAnno)
        : "GLB of @Format(CHAR) and @Format(TIME) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatCharAnno, formatFloatAnno), formatGeneralAnno)
        : "GLB of @Format(CHAR) and @Format(FLOAT) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatCharAnno, formatCharAndIntAnno), formatCharAnno)
        : "GLB of @Format(CHAR) and @Format(CHAR_AND_INT) is not @Format(CHAR)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatCharAnno, formatIntAndTimeAnno), formatGeneralAnno)
        : "GLB of @Format(CHAR) and @Format(INT_AND_TIME) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatCharAnno, formatNullAnno), formatCharAnno)
        : "GLB of @Format(CHAR) and @Format(NULL) is not @Format(CHAR)!";

    // GLB of INT and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatIntAnno, formatUnusedAnno), formatUnusedAnno)
        : "GLB of @Format(INT) and @Format(UNUSED) is not @Format(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatIntAnno, formatGeneralAnno), formatGeneralAnno)
        : "GLB of @Format(INT) and @Format(GENERAL) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatIntAnno, formatCharAnno), formatGeneralAnno)
        : "GLB of @Format(INT) and @Format(CHAR) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatIntAnno, formatIntAnno), formatIntAnno)
        : "GLB of @Format(INT) and @Format(INT) is not @Format(INT)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatIntAnno, formatTimeAnno), formatGeneralAnno)
        : "GLB of @Format(INT) and @Format(TIME) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatIntAnno, formatFloatAnno), formatGeneralAnno)
        : "GLB of @Format(INT) and @Format(FLOAT) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatIntAnno, formatCharAndIntAnno), formatIntAnno)
        : "GLB of @Format(INT) and @Format(CHAR_AND_INT) is not @Format(INT)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatIntAnno, formatIntAndTimeAnno), formatIntAnno)
        : "GLB of @Format(INT) and @Format(INT_AND_TIME) is not @Format(INT)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatIntAnno, formatNullAnno), formatIntAnno)
        : "GLB of @Format(INT) and @Format(NULL) is not @Format(INT)!";

    // GLB of TIME and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatTimeAnno, formatUnusedAnno), formatUnusedAnno)
        : "GLB of @Format(TIME) and @Format(UNUSED) is not @Format(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatTimeAnno, formatGeneralAnno), formatGeneralAnno)
        : "GLB of @Format(TIME) and @Format(GENERAL) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatTimeAnno, formatCharAnno), formatGeneralAnno)
        : "GLB of @Format(TIME) and @Format(CHAR) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatTimeAnno, formatIntAnno), formatGeneralAnno)
        : "GLB of @Format(TIME) and @Format(INT) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatTimeAnno, formatTimeAnno), formatTimeAnno)
        : "GLB of @Format(TIME) and @Format(TIME) is not @Format(TIME)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatTimeAnno, formatFloatAnno), formatGeneralAnno)
        : "GLB of @Format(TIME) and @Format(FLOAT) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatTimeAnno, formatCharAndIntAnno), formatGeneralAnno)
        : "GLB of @Format(TIME) and @Format(CHAR_AND_INT) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatTimeAnno, formatIntAndTimeAnno), formatTimeAnno)
        : "GLB of @Format(TIME) and @Format(INT_AND_TIME) is not @Format(TIME)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatTimeAnno, formatNullAnno), formatTimeAnno)
        : "GLB of @Format(TIME) and @Format(NULL) is not @Format(TIME)!";

    // GLB of FLOAT and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatFloatAnno, formatUnusedAnno), formatUnusedAnno)
        : "GLB of @Format(FLOAT) and @Format(UNUSED) is not @Format(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatFloatAnno, formatGeneralAnno), formatGeneralAnno)
        : "GLB of @Format(FLOAT) and @Format(GENERAL) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatFloatAnno, formatCharAnno), formatGeneralAnno)
        : "GLB of @Format(FLOAT) and @Format(CHAR) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatFloatAnno, formatIntAnno), formatGeneralAnno)
        : "GLB of @Format(FLOAT) and @Format(INT) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatFloatAnno, formatTimeAnno), formatGeneralAnno)
        : "GLB of @Format(FLOAT) and @Format(TIME) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatFloatAnno, formatFloatAnno), formatFloatAnno)
        : "GLB of @Format(FLOAT) and @Format(FLOAT) is not @Format(FLOAT)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatFloatAnno, formatCharAndIntAnno), formatGeneralAnno)
        : "GLB of @Format(FLOAT) and @Format(CHAR_AND_INT) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatFloatAnno, formatIntAndTimeAnno), formatGeneralAnno)
        : "GLB of @Format(FLOAT) and @Format(INT_AND_TIME) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatFloatAnno, formatNullAnno), formatFloatAnno)
        : "GLB of @Format(FLOAT) and @Format(NULL) is not @Format(FLOAT)!";

    // GLB of CHAR_AND_INT and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatCharAndIntAnno, formatUnusedAnno), formatUnusedAnno)
        : "GLB of @Format(CHAR_AND_INT) and @Format(UNUSED) is not @Format(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatCharAndIntAnno, formatGeneralAnno), formatGeneralAnno)
        : "GLB of @Format(CHAR_AND_INT) and @Format(GENERAL) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatCharAndIntAnno, formatCharAnno), formatCharAnno)
        : "GLB of @Format(CHAR_AND_INT) and @Format(CHAR) is not @Format(CHAR)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatCharAndIntAnno, formatIntAnno), formatIntAnno)
        : "GLB of @Format(CHAR_AND_INT) and @Format(INT) is not @Format(INT)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatCharAndIntAnno, formatTimeAnno), formatGeneralAnno)
        : "GLB of @Format(CHAR_AND_INT) and @Format(TIME) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatCharAndIntAnno, formatFloatAnno), formatGeneralAnno)
        : "GLB of @Format(CHAR_AND_INT) and @Format(FLOAT) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatCharAndIntAnno, formatCharAndIntAnno), formatCharAndIntAnno)
        : "GLB of @Format(CHAR_AND_INT) and @Format(CHAR_AND_INT) is not @Format(CHAR_AND_INT)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatCharAndIntAnno, formatIntAndTimeAnno), formatIntAnno)
        : "GLB of @Format(CHAR_AND_INT) and @Format(INT_AND_TIME) is not @Format(INT)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatCharAndIntAnno, formatNullAnno), formatCharAndIntAnno)
        : "GLB of @Format(CHAR_AND_INT) and @Format(NULL) is not @Format(CHAR_AND_INT)!";

    // GLB of INT_AND_TIME and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatIntAndTimeAnno, formatUnusedAnno), formatUnusedAnno)
        : "GLB of @Format(INT_AND_TIME) and @Format(UNUSED) is not @Format(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatIntAndTimeAnno, formatGeneralAnno), formatGeneralAnno)
        : "GLB of @Format(INT_AND_TIME) and @Format(GENERAL) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatIntAndTimeAnno, formatCharAnno), formatGeneralAnno)
        : "GLB of @Format(INT_AND_TIME) and @Format(CHAR) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatIntAndTimeAnno, formatIntAnno), formatIntAnno)
        : "GLB of @Format(INT_AND_TIME) and @Format(INT) is not @Format(INT)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatIntAndTimeAnno, formatTimeAnno), formatTimeAnno)
        : "GLB of @Format(INT_AND_TIME) and @Format(TIME) is not @Format(TIME)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatIntAndTimeAnno, formatFloatAnno), formatGeneralAnno)
        : "GLB of @Format(INT_AND_TIME) and @Format(FLOAT) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatIntAndTimeAnno, formatCharAndIntAnno), formatIntAnno)
        : "GLB of @Format(INT_AND_TIME) and @Format(CHAR_AND_INT) is not @Format(INT)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatIntAndTimeAnno, formatIntAndTimeAnno), formatIntAndTimeAnno)
        : "GLB of @Format(INT_AND_TIME) and @Format(INT_AND_TIME) is not @Format(INT_AND_TIME)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatIntAndTimeAnno, formatNullAnno), formatIntAndTimeAnno)
        : "GLB of @Format(INT_AND_TIME) and @Format(NULL) is not @Format(INT_AND_TIME)!";

    // GLB of NULL and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatNullAnno, formatUnusedAnno), formatUnusedAnno)
        : "GLB of @Format(NULL) and @Format(UNUSED) is not @Format(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatNullAnno, formatGeneralAnno), formatGeneralAnno)
        : "GLB of @Format(NULL) and @Format(GENERAL) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatNullAnno, formatCharAnno), formatCharAnno)
        : "GLB of @Format(NULL) and @Format(CHAR) is not @Format(CHAR)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatNullAnno, formatIntAnno), formatIntAnno)
        : "GLB of @Format(NULL) and @Format(INT) is not @Format(INT)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatNullAnno, formatTimeAnno), formatTimeAnno)
        : "GLB of @Format(NULL) and @Format(TIME) is not @Format(TIME)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatNullAnno, formatFloatAnno), formatFloatAnno)
        : "GLB of @Format(NULL) and @Format(FLOAT) is not @Format(FLOAT)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatNullAnno, formatCharAndIntAnno), formatCharAndIntAnno)
        : "GLB of @Format(NULL) and @Format(CHAR_AND_INT) is not @Format(CHAR_AND_INT)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatNullAnno, formatIntAndTimeAnno), formatIntAndTimeAnno)
        : "GLB of @Format(NULL) and @Format(INT_AND_TIME) is not @Format(INT_AND_TIME)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatNullAnno, formatNullAnno), formatNullAnno)
        : "GLB of @Format(NULL) and @Format(NULL) is not @Format(NULL)!";

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

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatTwoConvCat1, formatTwoConvCat2), formatTwoConvCat3)
        : "GLB of @Format([CHAR_AND_INT,FLOAT]) and @Format([INT,CHAR]) is not"
            + " @Format([INT,GENERAL])!";

    // Test that the GLB of two ConversionCategory arrays of different sizes is an array of the
    // smallest size of the two:

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatGeneralAnno, formatTwoConvCat1), formatGeneralAnno)
        : "GLB of @I18nFormat(GENERAL) and @I18nFormat([CHAR_AND_INT,FLOAT]) is not"
            + " @I18nFormat(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatTwoConvCat2, formatNullAnno), formatIntAnno)
        : "GLB of @I18nFormat([INT,CHAR]) and @I18nFormat(NULL) is not @I18nFormat(INT)!";

    // GLB of @UnknownFormat and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(UNKNOWNFORMAT, UNKNOWNFORMAT), UNKNOWNFORMAT)
        : "GLB of @UnknownFormat and @UnknownFormat is not @UnknownFormat!";
    assert AnnotationUtils.areSame(qh.greatestLowerBound(UNKNOWNFORMAT, FORMAT), FORMAT)
        : "GLB of @UnknownFormat and @Format(null) is not @Format(null)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(UNKNOWNFORMAT, formatUnusedAnno), formatUnusedAnno)
        : "GLB of @UnknownFormat and @Format(UNUSED) is not @Format(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(UNKNOWNFORMAT, INVALIDFORMAT), INVALIDFORMAT)
        : "GLB of @UnknownFormat and @InvalidFormat(null) is not @InvalidFormat(null)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(UNKNOWNFORMAT, invalidFormatWithMessage),
            invalidFormatWithMessage)
        : "GLB of @UnknownFormat and @InvalidFormat(\"Message\") is not"
            + " @InvalidFormat(\"Message\")!";
    assert AnnotationUtils.areSame(qh.greatestLowerBound(UNKNOWNFORMAT, FORMATBOTTOM), FORMATBOTTOM)
        : "GLB of @UnknownFormat and @FormatBottom is not @FormatBottom!";

    // GLB of @Format(null) and others

    assert AnnotationUtils.areSame(qh.greatestLowerBound(FORMAT, UNKNOWNFORMAT), FORMAT)
        : "GLB of @Format(null) and @UnknownFormat is not @Format(null)!";
    // Computing the GLB of @Format(null) and @Format(null) should never occur in practice.
    // Skipping this case as it causes an expected crash.
    // Computing the GLB of @Format(null) and @Format with a value should never occur in
    // practice. Skipping this case as it causes an expected crash.
    assert AnnotationUtils.areSame(qh.greatestLowerBound(FORMAT, INVALIDFORMAT), FORMATBOTTOM)
        : "GLB of @Format(null) and @InvalidFormat(null) is not @FormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(FORMAT, invalidFormatWithMessage), FORMATBOTTOM)
        : "GLB of @Format(null) and @InvalidFormat(\"Message\") is not @FormatBottom!";
    assert AnnotationUtils.areSame(qh.greatestLowerBound(FORMAT, FORMATBOTTOM), FORMATBOTTOM)
        : "GLB of @Format(null) and @FormatBottom is not @FormatBottom!";

    // GLB of @Format(UNUSED) and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatUnusedAnno, UNKNOWNFORMAT), formatUnusedAnno)
        : "GLB of @Format(UNUSED) and @UnknownFormat is not @Format(UNUSED)!";
    // Computing the GLB of @Format with a value and @Format(null) should never occur in
    // practice. Skipping this case as it causes an expected crash.
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatUnusedAnno, formatUnusedAnno), formatUnusedAnno)
        : "GLB of @Format(UNUSED) and @Format(UNUSED) is not @Format(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatUnusedAnno, INVALIDFORMAT), FORMATBOTTOM)
        : "GLB of @Format(UNUSED) and @InvalidFormat(null) is not @FormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatUnusedAnno, invalidFormatWithMessage), FORMATBOTTOM)
        : "GLB of @Format(UNUSED) and @InvalidFormat(\"Message\") is not @FormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatUnusedAnno, FORMATBOTTOM), FORMATBOTTOM)
        : "GLB of @Format(UNUSED) and @FormatBottom is not @FormatBottom!";

    // GLB of @InvalidFormat(null) and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(INVALIDFORMAT, UNKNOWNFORMAT), INVALIDFORMAT)
        : "GLB of @InvalidFormat(null) and @UnknownFormat is not @InvalidFormat(null)!";
    assert AnnotationUtils.areSame(qh.greatestLowerBound(INVALIDFORMAT, FORMAT), FORMATBOTTOM)
        : "GLB of @InvalidFormat(null) and @Format(null) is not @FormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(INVALIDFORMAT, formatUnusedAnno), FORMATBOTTOM)
        : "GLB of @InvalidFormat(null) and @Format(UNUSED) is not @FormatBottom!";
    assert AnnotationUtils.areSame(qh.greatestLowerBound(INVALIDFORMAT, FORMATBOTTOM), FORMATBOTTOM)
        : "GLB of @InvalidFormat(null) and @FormatBottom is not @FormatBottom!";

    // GLB of @InvalidFormat("Message") and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(invalidFormatWithMessage, UNKNOWNFORMAT),
            invalidFormatWithMessage)
        : "GLB of @InvalidFormat(\"Message\") and @UnknownFormat is not"
            + " @InvalidFormat(\"Message\")!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(invalidFormatWithMessage, FORMAT), FORMATBOTTOM)
        : "GLB of @InvalidFormat(\"Message\") and @Format(null) is not @FormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(invalidFormatWithMessage, formatUnusedAnno), FORMATBOTTOM)
        : "GLB of @InvalidFormat(\"Message\") and @Format(UNUSED) is not @FormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(invalidFormatWithMessage, invalidFormatWithMessage),
            invalidFormatWithMessage)
        : "GLB of @InvalidFormat(\"Message\") and @InvalidFormat(\"Message\") is not"
            + " @InvalidFormat(\"Message\")!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(invalidFormatWithMessage, invalidFormatWithMessage2),
            invalidFormatWithMessagesAnded)
        : "GLB of @InvalidFormat(\"Message\") and @InvalidFormat(\"Message2\") is not"
            + " @InvalidFormat(\"(\"Message\" and \"Message2\")\")!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(invalidFormatWithMessage, FORMATBOTTOM), FORMATBOTTOM)
        : "GLB of @InvalidFormat(\"Message\") and @FormatBottom is not @FormatBottom!";

    // GLB of @FormatBottom and others

    assert AnnotationUtils.areSame(qh.greatestLowerBound(FORMATBOTTOM, UNKNOWNFORMAT), FORMATBOTTOM)
        : "GLB of @FormatBottom and @UnknownFormat is not @FormatBottom!";
    assert AnnotationUtils.areSame(qh.greatestLowerBound(FORMATBOTTOM, FORMAT), FORMATBOTTOM)
        : "GLB of @FormatBottom and @Format(null) is not @FormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(FORMATBOTTOM, formatUnusedAnno), FORMATBOTTOM)
        : "GLB of @FormatBottom and @Format(UNUSED) is not @FormatBottom!";
    assert AnnotationUtils.areSame(qh.greatestLowerBound(FORMATBOTTOM, INVALIDFORMAT), FORMATBOTTOM)
        : "GLB of @FormatBottom and @InvalidFormat(null) is not @FormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(FORMATBOTTOM, invalidFormatWithMessage), FORMATBOTTOM)
        : "GLB of @FormatBottom and @InvalidFormat(\"Message\") is not @FormatBottom!";
    assert AnnotationUtils.areSame(qh.greatestLowerBound(FORMATBOTTOM, FORMATBOTTOM), FORMATBOTTOM)
        : "GLB of @FormatBottom and @FormatBottom is not @FormatBottom!";

    // ** LUB tests **

    // LUB of UNUSED and others

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatUnusedAnno, formatUnusedAnno), formatUnusedAnno)
        : "LUB of @Format(UNUSED) and @Format(UNUSED) is not @Format(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatUnusedAnno, formatGeneralAnno), formatGeneralAnno)
        : "LUB of @Format(UNUSED) and @Format(GENERAL) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatUnusedAnno, formatCharAnno), formatCharAnno)
        : "LUB of @Format(UNUSED) and @Format(CHAR) is not @Format(CHAR)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatUnusedAnno, formatIntAnno), formatIntAnno)
        : "LUB of @Format(UNUSED) and @Format(INT) is not @Format(INT)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatUnusedAnno, formatTimeAnno), formatTimeAnno)
        : "LUB of @Format(UNUSED) and @Format(TIME) is not @Format(TIME)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatUnusedAnno, formatFloatAnno), formatFloatAnno)
        : "LUB of @Format(UNUSED) and @Format(FLOAT) is not @Format(FLOAT)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatUnusedAnno, formatCharAndIntAnno), formatCharAndIntAnno)
        : "LUB of @Format(UNUSED) and @Format(CHAR_AND_INT) is not @Format(CHAR_AND_INT)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatUnusedAnno, formatIntAndTimeAnno), formatIntAndTimeAnno)
        : "LUB of @Format(UNUSED) and @Format(INT_AND_TIME) is not @Format(INT_AND_TIME)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatUnusedAnno, formatNullAnno), formatNullAnno)
        : "LUB of @Format(UNUSED) and @Format(NULL) is not @Format(NULL)!";

    // LUB of GENERAL and others

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatGeneralAnno, formatUnusedAnno), formatGeneralAnno)
        : "LUB of @Format(GENERAL) and @Format(UNUSED) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatGeneralAnno, formatGeneralAnno), formatGeneralAnno)
        : "LUB of @Format(GENERAL) and @Format(GENERAL) is not @Format(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatGeneralAnno, formatCharAnno), formatCharAnno)
        : "LUB of @Format(GENERAL) and @Format(CHAR) is not @Format(CHAR)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatGeneralAnno, formatIntAnno), formatIntAnno)
        : "LUB of @Format(GENERAL) and @Format(INT) is not @Format(INT)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatGeneralAnno, formatTimeAnno), formatTimeAnno)
        : "LUB of @Format(GENERAL) and @Format(TIME) is not @Format(TIME)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatGeneralAnno, formatFloatAnno), formatFloatAnno)
        : "LUB of @Format(GENERAL) and @Format(FLOAT) is not @Format(FLOAT)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatGeneralAnno, formatCharAndIntAnno), formatCharAndIntAnno)
        : "LUB of @Format(GENERAL) and @Format(CHAR_AND_INT) is not @Format(CHAR_AND_INT)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatGeneralAnno, formatIntAndTimeAnno), formatIntAndTimeAnno)
        : "LUB of @Format(GENERAL) and @Format(INT_AND_TIME) is not @Format(INT_AND_TIME)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatGeneralAnno, formatNullAnno), formatNullAnno)
        : "LUB of @Format(GENERAL) and @Format(NULL) is not @Format(NULL)!";

    // LUB of CHAR and others

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatCharAnno, formatUnusedAnno), formatCharAnno)
        : "LUB of @Format(CHAR) and @Format(UNUSED) is not @Format(CHAR)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatCharAnno, formatGeneralAnno), formatCharAnno)
        : "LUB of @Format(CHAR) and @Format(GENERAL) is not @Format(CHAR)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatCharAnno, formatCharAnno), formatCharAnno)
        : "LUB of @Format(CHAR) and @Format(CHAR) is not @Format(CHAR)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatCharAnno, formatIntAnno), formatCharAndIntAnno)
        : "LUB of @Format(CHAR) and @Format(INT) is not @Format(CHAR_AND_INT)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatCharAnno, formatTimeAnno), formatNullAnno)
        : "LUB of @Format(CHAR) and @Format(TIME) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatCharAnno, formatFloatAnno), formatNullAnno)
        : "LUB of @Format(CHAR) and @Format(FLOAT) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatCharAnno, formatCharAndIntAnno), formatCharAndIntAnno)
        : "LUB of @Format(CHAR) and @Format(CHAR_AND_INT) is not @Format(CHAR_AND_INT)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatCharAnno, formatIntAndTimeAnno), formatNullAnno)
        : "LUB of @Format(CHAR) and @Format(INT_AND_TIME) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatCharAnno, formatNullAnno), formatNullAnno)
        : "LUB of @Format(CHAR) and @Format(NULL) is not @Format(NULL)!";

    // LUB of INT and others

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatIntAnno, formatUnusedAnno), formatIntAnno)
        : "LUB of @Format(INT) and @Format(UNUSED) is not @Format(INT)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatIntAnno, formatGeneralAnno), formatIntAnno)
        : "LUB of @Format(INT) and @Format(GENERAL) is not @Format(INT)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatIntAnno, formatCharAnno), formatCharAndIntAnno)
        : "LUB of @Format(INT) and @Format(CHAR) is not @Format(CHAR_AND_INT)!";
    assert AnnotationUtils.areSame(qh.leastUpperBound(formatIntAnno, formatIntAnno), formatIntAnno)
        : "LUB of @Format(INT) and @Format(INT) is not @Format(INT)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatIntAnno, formatTimeAnno), formatIntAndTimeAnno)
        : "LUB of @Format(INT) and @Format(TIME) is not @Format(INT_AND_TIME)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatIntAnno, formatFloatAnno), formatNullAnno)
        : "LUB of @Format(INT) and @Format(FLOAT) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatIntAnno, formatCharAndIntAnno), formatCharAndIntAnno)
        : "LUB of @Format(INT) and @Format(CHAR_AND_INT) is not @Format(CHAR_AND_INT)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatIntAnno, formatIntAndTimeAnno), formatIntAndTimeAnno)
        : "LUB of @Format(INT) and @Format(INT_AND_TIME) is not @Format(INT_AND_TIME)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatIntAnno, formatNullAnno), formatNullAnno)
        : "LUB of @Format(INT) and @Format(NULL) is not @Format(NULL)!";

    // LUB of TIME and others

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatTimeAnno, formatUnusedAnno), formatTimeAnno)
        : "LUB of @Format(TIME) and @Format(UNUSED) is not @Format(TIME)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatTimeAnno, formatGeneralAnno), formatTimeAnno)
        : "LUB of @Format(TIME) and @Format(GENERAL) is not @Format(TIME)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatTimeAnno, formatCharAnno), formatNullAnno)
        : "LUB of @Format(TIME) and @Format(CHAR) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatTimeAnno, formatIntAnno), formatIntAndTimeAnno)
        : "LUB of @Format(TIME) and @Format(INT) is not @Format(INT_AND_TIME)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatTimeAnno, formatTimeAnno), formatTimeAnno)
        : "LUB of @Format(TIME) and @Format(TIME) is not @Format(TIME)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatTimeAnno, formatFloatAnno), formatNullAnno)
        : "LUB of @Format(TIME) and @Format(FLOAT) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatTimeAnno, formatCharAndIntAnno), formatNullAnno)
        : "LUB of @Format(TIME) and @Format(CHAR_AND_INT) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatTimeAnno, formatIntAndTimeAnno), formatIntAndTimeAnno)
        : "LUB of @Format(TIME) and @Format(INT_AND_TIME) is not @Format(INT_AND_TIME)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatTimeAnno, formatNullAnno), formatNullAnno)
        : "LUB of @Format(TIME) and @Format(NULL) is not @Format(NULL)!";

    // LUB of FLOAT and others

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatFloatAnno, formatUnusedAnno), formatFloatAnno)
        : "LUB of @Format(FLOAT) and @Format(UNUSED) is not @Format(FLOAT)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatFloatAnno, formatGeneralAnno), formatFloatAnno)
        : "LUB of @Format(FLOAT) and @Format(GENERAL) is not @Format(FLOAT)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatFloatAnno, formatCharAnno), formatNullAnno)
        : "LUB of @Format(FLOAT) and @Format(CHAR) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatFloatAnno, formatIntAnno), formatNullAnno)
        : "LUB of @Format(FLOAT) and @Format(INT) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatFloatAnno, formatTimeAnno), formatNullAnno)
        : "LUB of @Format(FLOAT) and @Format(TIME) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatFloatAnno, formatFloatAnno), formatFloatAnno)
        : "LUB of @Format(FLOAT) and @Format(FLOAT) is not @Format(FLOAT)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatFloatAnno, formatCharAndIntAnno), formatNullAnno)
        : "LUB of @Format(FLOAT) and @Format(CHAR_AND_INT) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatFloatAnno, formatIntAndTimeAnno), formatNullAnno)
        : "LUB of @Format(FLOAT) and @Format(INT_AND_TIME) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatFloatAnno, formatNullAnno), formatNullAnno)
        : "LUB of @Format(FLOAT) and @Format(NULL) is not @Format(NULL)!";

    // LUB of CHAR_AND_INT and others

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatCharAndIntAnno, formatUnusedAnno), formatCharAndIntAnno)
        : "LUB of @Format(CHAR_AND_INT) and @Format(UNUSED) is not @Format(CHAR_AND_INT)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatCharAndIntAnno, formatGeneralAnno), formatCharAndIntAnno)
        : "LUB of @Format(CHAR_AND_INT) and @Format(GENERAL) is not @Format(CHAR_AND_INT)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatCharAndIntAnno, formatCharAnno), formatCharAndIntAnno)
        : "LUB of @Format(CHAR_AND_INT) and @Format(CHAR) is not @Format(CHAR_AND_INT)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatCharAndIntAnno, formatIntAnno), formatCharAndIntAnno)
        : "LUB of @Format(CHAR_AND_INT) and @Format(INT) is not @Format(CHAR_AND_INT)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatCharAndIntAnno, formatTimeAnno), formatNullAnno)
        : "LUB of @Format(CHAR_AND_INT) and @Format(TIME) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatCharAndIntAnno, formatFloatAnno), formatNullAnno)
        : "LUB of @Format(CHAR_AND_INT) and @Format(FLOAT) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatCharAndIntAnno, formatCharAndIntAnno), formatCharAndIntAnno)
        : "LUB of @Format(CHAR_AND_INT) and @Format(CHAR_AND_INT) is not @Format(CHAR_AND_INT)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatCharAndIntAnno, formatIntAndTimeAnno), formatNullAnno)
        : "LUB of @Format(CHAR_AND_INT) and @Format(INT_AND_TIME) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatCharAndIntAnno, formatNullAnno), formatNullAnno)
        : "LUB of @Format(CHAR_AND_INT) and @Format(NULL) is not @Format(NULL)!";

    // LUB of INT_AND_TIME and others

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatIntAndTimeAnno, formatUnusedAnno), formatIntAndTimeAnno)
        : "LUB of @Format(INT_AND_TIME) and @Format(UNUSED) is not @Format(INT_AND_TIME)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatIntAndTimeAnno, formatGeneralAnno), formatIntAndTimeAnno)
        : "LUB of @Format(INT_AND_TIME) and @Format(GENERAL) is not @Format(INT_AND_TIME)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatIntAndTimeAnno, formatCharAnno), formatNullAnno)
        : "LUB of @Format(INT_AND_TIME) and @Format(CHAR) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatIntAndTimeAnno, formatIntAnno), formatIntAndTimeAnno)
        : "LUB of @Format(INT_AND_TIME) and @Format(INT) is not @Format(INT_AND_TIME)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatIntAndTimeAnno, formatTimeAnno), formatIntAndTimeAnno)
        : "LUB of @Format(INT_AND_TIME) and @Format(TIME) is not @Format(INT_AND_TIME)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatIntAndTimeAnno, formatFloatAnno), formatNullAnno)
        : "LUB of @Format(INT_AND_TIME) and @Format(FLOAT) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatIntAndTimeAnno, formatCharAndIntAnno), formatNullAnno)
        : "LUB of @Format(INT_AND_TIME) and @Format(CHAR_AND_INT) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatIntAndTimeAnno, formatIntAndTimeAnno), formatIntAndTimeAnno)
        : "LUB of @Format(INT_AND_TIME) and @Format(INT_AND_TIME) is not @Format(INT_AND_TIME)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatIntAndTimeAnno, formatNullAnno), formatNullAnno)
        : "LUB of @Format(INT_AND_TIME) and @Format(NULL) is not @Format(NULL)!";

    // LUB of NULL and others

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatNullAnno, formatUnusedAnno), formatNullAnno)
        : "LUB of @Format(NULL) and @Format(UNUSED) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatNullAnno, formatGeneralAnno), formatNullAnno)
        : "LUB of @Format(NULL) and @Format(GENERAL) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatNullAnno, formatCharAnno), formatNullAnno)
        : "LUB of @Format(NULL) and @Format(CHAR) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatNullAnno, formatIntAnno), formatNullAnno)
        : "LUB of @Format(NULL) and @Format(INT) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatNullAnno, formatTimeAnno), formatNullAnno)
        : "LUB of @Format(NULL) and @Format(TIME) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatNullAnno, formatFloatAnno), formatNullAnno)
        : "LUB of @Format(NULL) and @Format(FLOAT) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatNullAnno, formatCharAndIntAnno), formatNullAnno)
        : "LUB of @Format(NULL) and @Format(CHAR_AND_INT) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatNullAnno, formatIntAndTimeAnno), formatNullAnno)
        : "LUB of @Format(NULL) and @Format(INT_AND_TIME) is not @Format(NULL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatNullAnno, formatNullAnno), formatNullAnno)
        : "LUB of @Format(NULL) and @Format(NULL) is not @Format(NULL)!";

    // Now test with two ConversionCategory at a time:

    cc2[0] = ConversionCategory.CHAR_AND_INT;
    cc2[1] = ConversionCategory.NULL;
    AnnotationMirror formatTwoConvCat4 = treeUtil.categoriesToFormatAnnotation(cc2);
    cc2[0] = ConversionCategory.NULL;
    cc2[1] = ConversionCategory.CHAR;
    AnnotationMirror formatTwoConvCat5 = treeUtil.categoriesToFormatAnnotation(cc2);

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatTwoConvCat1, formatTwoConvCat2), formatTwoConvCat4)
        : "LUB of @Format([CHAR_AND_INT,FLOAT]) and @Format([INT,CHAR]) is not"
            + " @Format([CHAR_AND_INT,NULL])!";

    // Test that the LUB of two ConversionCategory arrays of different sizes is an array of the
    // largest size of the two:

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatGeneralAnno, formatTwoConvCat1), formatTwoConvCat1)
        : "LUB of @I18nFormat(GENERAL) and @I18nFormat([CHAR_AND_INT,FLOAT]) is not"
            + " @I18nFormat([CHAR_AND_INT,FLOAT])!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatTwoConvCat2, formatNullAnno), formatTwoConvCat5)
        : "LUB of @I18nFormat([INT,CHAR]) and @I18nFormat(NULL) is not @I18nFormat([NULL,CHAR])!";

    // LUB of @UnknownFormat and others

    assert AnnotationUtils.areSame(qh.leastUpperBound(UNKNOWNFORMAT, UNKNOWNFORMAT), UNKNOWNFORMAT)
        : "LUB of @UnknownFormat and @UnknownFormat is not @UnknownFormat!";
    assert AnnotationUtils.areSame(qh.leastUpperBound(UNKNOWNFORMAT, FORMAT), UNKNOWNFORMAT)
        : "LUB of @UnknownFormat and @Format(null) is not @UnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(UNKNOWNFORMAT, formatUnusedAnno), UNKNOWNFORMAT)
        : "LUB of @UnknownFormat and @Format(UNUSED) is not @UnknownFormat!";
    assert AnnotationUtils.areSame(qh.leastUpperBound(UNKNOWNFORMAT, INVALIDFORMAT), UNKNOWNFORMAT)
        : "LUB of @UnknownFormat and @InvalidFormat(null) is not @UnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(UNKNOWNFORMAT, invalidFormatWithMessage), UNKNOWNFORMAT)
        : "LUB of @UnknownFormat and @InvalidFormat(\"Message\") is not @UnknownFormat!";
    assert AnnotationUtils.areSame(qh.leastUpperBound(UNKNOWNFORMAT, FORMATBOTTOM), UNKNOWNFORMAT)
        : "LUB of @UnknownFormat and @FormatBottom is not @UnknownFormat!";

    // LUB of @Format(null) and others

    assert AnnotationUtils.areSame(qh.leastUpperBound(FORMAT, UNKNOWNFORMAT), UNKNOWNFORMAT)
        : "LUB of @Format(null) and @UnknownFormat is not @UnknownFormat!";
    // Computing the LUB of @Format(null) and @Format(null) should never occur in practice.
    // Skipping this case as it causes an expected crash.
    // Computing the LUB of @Format(null) and @Format with a value should never occur in
    // practice. Skipping this case as it causes an expected crash.
    assert AnnotationUtils.areSame(qh.leastUpperBound(FORMAT, INVALIDFORMAT), UNKNOWNFORMAT)
        : "LUB of @Format(null) and @InvalidFormat(null) is not @UnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(FORMAT, invalidFormatWithMessage), UNKNOWNFORMAT)
        : "LUB of @Format(null) and @InvalidFormat(\"Message\") is not @UnknownFormat!";
    assert AnnotationUtils.areSame(qh.leastUpperBound(FORMAT, FORMATBOTTOM), FORMAT)
        : "LUB of @Format(null) and @FormatBottom is not @Format(null)!";

    // LUB of @Format(UNUSED) and others

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatUnusedAnno, UNKNOWNFORMAT), UNKNOWNFORMAT)
        : "LUB of @Format(UNUSED) and @UnknownFormat is not @UnknownFormat!";
    // Computing the LUB of @Format with a value and @Format(null) should never occur in
    // practice. Skipping this case as it causes an expected crash.
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatUnusedAnno, formatUnusedAnno), formatUnusedAnno)
        : "LUB of @Format(UNUSED) and @Format(UNUSED) is not @Format(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatUnusedAnno, INVALIDFORMAT), UNKNOWNFORMAT)
        : "LUB of @Format(UNUSED) and @InvalidFormat(null) is not @UnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatUnusedAnno, invalidFormatWithMessage), UNKNOWNFORMAT)
        : "LUB of @Format(UNUSED) and @InvalidFormat(\"Message\") is not @UnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatUnusedAnno, FORMATBOTTOM), formatUnusedAnno)
        : "LUB of @Format(UNUSED) and @FormatBottom is not @Format(UNUSED)!";

    // LUB of @InvalidFormat(null) and others

    assert AnnotationUtils.areSame(qh.leastUpperBound(INVALIDFORMAT, UNKNOWNFORMAT), UNKNOWNFORMAT)
        : "LUB of @InvalidFormat(null) and @UnknownFormat is not @UnknownFormat!";
    assert AnnotationUtils.areSame(qh.leastUpperBound(INVALIDFORMAT, FORMAT), UNKNOWNFORMAT)
        : "LUB of @InvalidFormat(null) and @Format(null) is not @UnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(INVALIDFORMAT, formatUnusedAnno), UNKNOWNFORMAT)
        : "LUB of @InvalidFormat(null) and @Format(UNUSED) is not @UnknownFormat!";
    assert AnnotationUtils.areSame(qh.leastUpperBound(INVALIDFORMAT, FORMATBOTTOM), INVALIDFORMAT)
        : "LUB of @InvalidFormat(null) and @FormatBottom is not @InvalidFormat(null)!";

    // LUB of @InvalidFormat("Message") and others

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(invalidFormatWithMessage, UNKNOWNFORMAT), UNKNOWNFORMAT)
        : "LUB of @InvalidFormat(\"Message\") and @UnknownFormat is not @UnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(invalidFormatWithMessage, FORMAT), UNKNOWNFORMAT)
        : "LUB of @InvalidFormat(\"Message\") and @Format(null) is not @UnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(invalidFormatWithMessage, formatUnusedAnno), UNKNOWNFORMAT)
        : "LUB of @InvalidFormat(\"Message\") and @Format(UNUSED) is not @UnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(invalidFormatWithMessage, invalidFormatWithMessage),
            invalidFormatWithMessage)
        : "LUB of @InvalidFormat(\"Message\") and @InvalidFormat(\"Message\") is not"
            + " @InvalidFormat(\"Message\")!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(invalidFormatWithMessage, invalidFormatWithMessage2),
            invalidFormatWithMessagesOred)
        : "LUB of @InvalidFormat(\"Message\") and @InvalidFormat(\"Message2\") is not"
            + " @InvalidFormat(\"(\"Message\" or \"Message2\")\")!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(invalidFormatWithMessage, FORMATBOTTOM), invalidFormatWithMessage)
        : "LUB of @InvalidFormat(\"Message\") and @FormatBottom is not"
            + " @InvalidFormat(\"Message\")!";

    // LUB of @FormatBottom and others

    assert AnnotationUtils.areSame(qh.leastUpperBound(FORMATBOTTOM, UNKNOWNFORMAT), UNKNOWNFORMAT)
        : "LUB of @FormatBottom and @UnknownFormat is not @UnknownFormat!";
    assert AnnotationUtils.areSame(qh.leastUpperBound(FORMATBOTTOM, FORMAT), FORMAT)
        : "LUB of @FormatBottom and @Format(null) is not @Format(null)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(FORMATBOTTOM, formatUnusedAnno), formatUnusedAnno)
        : "LUB of @FormatBottom and @Format(UNUSED) is not @Format(UNUSED)!";
    assert AnnotationUtils.areSame(qh.leastUpperBound(FORMATBOTTOM, INVALIDFORMAT), INVALIDFORMAT)
        : "LUB of @FormatBottom and @InvalidFormat(null) is not @InvalidFormat(null)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(FORMATBOTTOM, invalidFormatWithMessage), invalidFormatWithMessage)
        : "LUB of @FormatBottom and @InvalidFormat(\"Message\") is not"
            + " @InvalidFormat(\"Message\")!";
    assert AnnotationUtils.areSame(qh.leastUpperBound(FORMATBOTTOM, FORMATBOTTOM), FORMATBOTTOM)
        : "LUB of @FormatBottom and @FormatBottom is not @FormatBottom!";
  }
}
