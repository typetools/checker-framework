package org.checkerframework.checker.testchecker.lubglb;

// Test case for issues 723 and 756.
// https://github.com/typetools/checker-framework/issues/723
// https://github.com/typetools/checker-framework/issues/756

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.i18nformatter.I18nFormatterAnnotatedTypeFactory;
import org.checkerframework.checker.i18nformatter.I18nFormatterChecker;
import org.checkerframework.checker.i18nformatter.I18nFormatterTreeUtil;
import org.checkerframework.checker.i18nformatter.I18nFormatterVisitor;
import org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.checkerframework.checker.i18nformatter.qual.I18nFormatBottom;
import org.checkerframework.checker.i18nformatter.qual.I18nFormatFor;
import org.checkerframework.checker.i18nformatter.qual.I18nInvalidFormat;
import org.checkerframework.checker.i18nformatter.qual.I18nUnknownFormat;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * This class tests the implementation of GLB computation in the I18n Format String Checker (see
 * issue 723), but it does not test for the crash that occurs if I18nFormatterAnnotatedTypeFactory
 * does not override greatestLowerBound. That is done by tests/all-systems/Issue691.java. It also
 * tests the implementation of LUB computation in the I18n Format String Checker.
 */
public class I18nFormatterLubGlbChecker extends I18nFormatterChecker {
  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new I18nFormatterVisitor(this) {
      @Override
      protected I18nFormatterAnnotatedTypeFactory createTypeFactory() {
        return new I18nFormatterLubGlbAnnotatedTypeFactory(checker);
      }
    };
  }

  /** I18nFormatterLubGlbAnnotatedTypeFactory. */
  private static class I18nFormatterLubGlbAnnotatedTypeFactory
      extends I18nFormatterAnnotatedTypeFactory {

    /**
     * Constructor.
     *
     * @param checker checker
     */
    public I18nFormatterLubGlbAnnotatedTypeFactory(BaseTypeChecker checker) {
      super(checker);
      postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
      return new HashSet<>(
          Arrays.asList(
              I18nUnknownFormat.class,
              I18nFormatBottom.class,
              I18nFormat.class,
              I18nInvalidFormat.class,
              I18nFormatFor.class));
    }
  }

  @SuppressWarnings("checkstyle:localvariablename")
  @Override
  public void initChecker() {
    super.initChecker();
    I18nFormatterTreeUtil treeUtil = new I18nFormatterTreeUtil(this);

    Elements elements = getElementUtils();
    AnnotationMirror I18NUNKNOWNFORMAT =
        AnnotationBuilder.fromClass(elements, I18nUnknownFormat.class);
    AnnotationMirror I18NFORMAT =
        AnnotationBuilder.fromClass(
            elements,
            I18nFormat.class,
            AnnotationBuilder.elementNamesValues("value", new I18nConversionCategory[0]));
    AnnotationMirror I18NINVALIDFORMAT =
        AnnotationBuilder.fromClass(
            elements,
            I18nInvalidFormat.class,
            AnnotationBuilder.elementNamesValues("value", "dummy"));
    AnnotationMirror I18NFORMATFOR =
        AnnotationBuilder.fromClass(
            elements, I18nFormatFor.class, AnnotationBuilder.elementNamesValues("value", "dummy"));
    AnnotationMirror I18NFORMATBOTTOM =
        AnnotationBuilder.fromClass(elements, I18nFormatBottom.class);

    AnnotationBuilder builder = new AnnotationBuilder(processingEnv, I18nInvalidFormat.class);
    builder.setValue("value", "Message");
    AnnotationMirror i18nInvalidFormatWithMessage = builder.build();

    builder = new AnnotationBuilder(processingEnv, I18nInvalidFormat.class);
    builder.setValue("value", "Message2");
    AnnotationMirror i18nInvalidFormatWithMessage2 = builder.build();

    builder = new AnnotationBuilder(processingEnv, I18nInvalidFormat.class);
    builder.setValue("value", "(\"Message\" or \"Message2\")");
    AnnotationMirror i18nInvalidFormatWithMessagesOred = builder.build();

    builder = new AnnotationBuilder(processingEnv, I18nInvalidFormat.class);
    builder.setValue("value", "(\"Message\" and \"Message2\")");
    AnnotationMirror i18nInvalidFormatWithMessagesAnded = builder.build();

    builder = new AnnotationBuilder(processingEnv, I18nFormatFor.class);
    builder.setValue("value", "#1");
    AnnotationMirror i18nFormatForWithValue1 = builder.build();

    builder = new AnnotationBuilder(processingEnv, I18nFormatFor.class);
    builder.setValue("value", "#2");
    AnnotationMirror i18nFormatForWithValue2 = builder.build();

    I18nConversionCategory[] cc = new I18nConversionCategory[1];

    cc[0] = I18nConversionCategory.UNUSED;
    AnnotationMirror i18nFormatUnusedAnno = treeUtil.categoriesToFormatAnnotation(cc);
    cc[0] = I18nConversionCategory.GENERAL;
    AnnotationMirror i18nFormatGeneralAnno = treeUtil.categoriesToFormatAnnotation(cc);
    cc[0] = I18nConversionCategory.DATE;
    AnnotationMirror i18nFormatDateAnno = treeUtil.categoriesToFormatAnnotation(cc);
    cc[0] = I18nConversionCategory.NUMBER;
    AnnotationMirror i18nFormatNumberAnno = treeUtil.categoriesToFormatAnnotation(cc);

    QualifierHierarchy qh = ((BaseTypeVisitor<?>) visitor).getTypeFactory().getQualifierHierarchy();

    // ** GLB tests **

    // GLB of UNUSED and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatUnusedAnno, i18nFormatUnusedAnno), i18nFormatUnusedAnno)
        : "GLB of @I18nFormat(UNUSED) and @I18nFormat(UNUSED) is not @I18nFormat(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatUnusedAnno, i18nFormatGeneralAnno),
            i18nFormatUnusedAnno)
        : "GLB of @I18nFormat(UNUSED) and @I18nFormat(GENERAL) is not @I18nFormat(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatUnusedAnno, i18nFormatDateAnno), i18nFormatUnusedAnno)
        : "GLB of @I18nFormat(UNUSED) and @I18nFormat(DATE) is not @I18nFormat(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatUnusedAnno, i18nFormatNumberAnno), i18nFormatUnusedAnno)
        : "GLB of @I18nFormat(UNUSED) and @I18nFormat(NUMBER) is not @I18nFormat(UNUSED)!";

    // GLB of GENERAL and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatGeneralAnno, i18nFormatUnusedAnno),
            i18nFormatUnusedAnno)
        : "GLB of @I18nFormat(GENERAL) and @I18nFormat(UNUSED) is not @I18nFormat(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatGeneralAnno, i18nFormatGeneralAnno),
            i18nFormatGeneralAnno)
        : "GLB of @I18nFormat(GENERAL) and @I18nFormat(GENERAL) is not @I18nFormat(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatGeneralAnno, i18nFormatDateAnno), i18nFormatGeneralAnno)
        : "GLB of @I18nFormat(GENERAL) and @I18nFormat(DATE) is not @I18nFormat(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatGeneralAnno, i18nFormatNumberAnno),
            i18nFormatGeneralAnno)
        : "GLB of @I18nFormat(GENERAL) and @I18nFormat(NUMBER) is not @I18nFormat(GENERAL)!";

    // GLB of DATE and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatDateAnno, i18nFormatUnusedAnno), i18nFormatUnusedAnno)
        : "GLB of @I18nFormat(DATE) and @I18nFormat(UNUSED) is not @I18nFormat(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatDateAnno, i18nFormatGeneralAnno), i18nFormatGeneralAnno)
        : "GLB of @I18nFormat(DATE) and @I18nFormat(GENERAL) is not @I18nFormat(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatDateAnno, i18nFormatDateAnno), i18nFormatDateAnno)
        : "GLB of @I18nFormat(DATE) and @I18nFormat(DATE) is not @I18nFormat(DATE)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatDateAnno, i18nFormatNumberAnno), i18nFormatDateAnno)
        : "GLB of @I18nFormat(DATE) and @I18nFormat(NUMBER) is not @I18nFormat(DATE)!";

    // GLB of NUMBER and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatNumberAnno, i18nFormatUnusedAnno), i18nFormatUnusedAnno)
        : "GLB of @I18nFormat(NUMBER) and @I18nFormat(UNUSED) is not @I18nFormat(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatNumberAnno, i18nFormatGeneralAnno),
            i18nFormatGeneralAnno)
        : "GLB of @I18nFormat(NUMBER) and @I18nFormat(GENERAL) is not @I18nFormat(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatNumberAnno, i18nFormatDateAnno), i18nFormatDateAnno)
        : "GLB of @I18nFormat(NUMBER) and @I18nFormat(DATE) is not @I18nFormat(DATE)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatNumberAnno, i18nFormatNumberAnno), i18nFormatNumberAnno)
        : "GLB of @I18nFormat(NUMBER) and @I18nFormat(NUMBER) is not @I18nFormat(NUMBER)!";

    // Now test with two I18nConversionCategory at a time:

    I18nConversionCategory[] cc2 = new I18nConversionCategory[2];

    cc2[0] = I18nConversionCategory.DATE;
    cc2[1] = I18nConversionCategory.DATE;
    AnnotationMirror formatTwoConvCat1 = treeUtil.categoriesToFormatAnnotation(cc2);
    cc2[0] = I18nConversionCategory.UNUSED;
    cc2[1] = I18nConversionCategory.NUMBER;
    AnnotationMirror formatTwoConvCat2 = treeUtil.categoriesToFormatAnnotation(cc2);
    cc2[0] = I18nConversionCategory.UNUSED;
    cc2[1] = I18nConversionCategory.DATE;
    AnnotationMirror formatTwoConvCat3 = treeUtil.categoriesToFormatAnnotation(cc2);

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatTwoConvCat1, formatTwoConvCat2), formatTwoConvCat3)
        : "GLB of @I18nFormat([DATE,DATE]) and @I18nFormat([UNUSED,NUMBER]) is not"
            + " @I18nFormat([UNUSED,DATE])!";

    // Test that the GLB of two I18nConversionCategory arrays of different sizes is an array of
    // the smallest size of the two:

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatGeneralAnno, formatTwoConvCat1), i18nFormatGeneralAnno)
        : "GLB of @I18nFormat(GENERAL) and @I18nFormat([DATE,DATE]) is not @I18nFormat(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(formatTwoConvCat2, i18nFormatDateAnno), i18nFormatUnusedAnno)
        : "GLB of @I18nFormat([UNUSED,NUMBER]) and @I18nFormat(DATE) is not @I18nFormat(UNUSED)!";

    // GLB of two distinct @I18nFormatFor(...) annotations is @I18nFormatBottom

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatForWithValue1, i18nFormatForWithValue2),
            I18NFORMATBOTTOM)
        : "GLB of @I18nFormatFor(\"#1\") and @I18nFormatFor(\"#2\") is not @I18nFormatBottom!";

    // GLB of @I18nUnknownFormat and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NUNKNOWNFORMAT, I18NUNKNOWNFORMAT), I18NUNKNOWNFORMAT)
        : "GLB of @I18nUnknownFormat and @I18nUnknownFormat is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NUNKNOWNFORMAT, I18NFORMAT), I18NFORMAT)
        : "GLB of @I18nUnknownFormat and @I18nFormat(null) is not @I18nFormat(null)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NUNKNOWNFORMAT, i18nFormatUnusedAnno), i18nFormatUnusedAnno)
        : "GLB of @I18nUnknownFormat and @I18nFormat(UNUSED) is not @I18nFormat(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NUNKNOWNFORMAT, I18NINVALIDFORMAT), I18NINVALIDFORMAT)
        : "GLB of @I18nUnknownFormat and @I18nInvalidFormat(null) is not @I18nInvalidFormat(null)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NUNKNOWNFORMAT, i18nInvalidFormatWithMessage),
            i18nInvalidFormatWithMessage)
        : "GLB of @I18nUnknownFormat and @I18nInvalidFormat(\"Message\") is not"
            + " @I18nInvalidFormat(\"Message\")!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NUNKNOWNFORMAT, I18NFORMATFOR), I18NFORMATFOR)
        : "GLB of @I18nUnknownFormat and @I18nFormatFor(null) is not @I18nFormatFor(null)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NUNKNOWNFORMAT, i18nFormatForWithValue1),
            i18nFormatForWithValue1)
        : "GLB of @I18nUnknownFormat and @I18nFormatFor(\"#1\") is not @I18nFormatFor(\"#1\")!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NUNKNOWNFORMAT, I18NFORMATBOTTOM), I18NFORMATBOTTOM)
        : "GLB of @I18nUnknownFormat and @I18nFormatBottom is not @I18nFormatBottom!";

    // GLB of @I18nFormat(null) and others

    assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NFORMAT, I18NUNKNOWNFORMAT), I18NFORMAT)
        : "GLB of @I18nFormat(null) and @I18nUnknownFormat is not @I18nFormat(null)!";
    // Computing the GLB of @I18nFormat(null) and @I18nFormat(null) should never occur in
    // practice. Skipping this case as it causes an expected crash.
    // Computing the GLB of @I18nFormat(null) and @I18nFormat with a value should never occur in
    // practice. Skipping this case as it causes an expected crash.
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NFORMAT, I18NINVALIDFORMAT), I18NFORMATBOTTOM)
        : "GLB of @I18nFormat(null) and @I18nInvalidFormat(null) is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NFORMAT, i18nInvalidFormatWithMessage), I18NFORMATBOTTOM)
        : "GLB of @I18nFormat(null) and @I18nInvalidFormat(\"Message\") is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NFORMAT, I18NFORMATFOR), I18NFORMATBOTTOM)
        : "GLB of @I18nFormat(null) and @I18nFormatFor(null) is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NFORMAT, i18nFormatForWithValue1), I18NFORMATBOTTOM)
        : "GLB of @I18nFormat(null) and @I18nFormatFor(\"#1\") is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NFORMAT, I18NFORMATBOTTOM), I18NFORMATBOTTOM)
        : "GLB of @I18nFormat(null) and @I18nFormatBottom is not @I18nFormatBottom!";

    // GLB of @I18nFormat(UNUSED) and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatUnusedAnno, I18NUNKNOWNFORMAT), i18nFormatUnusedAnno)
        : "GLB of @I18nFormat(UNUSED) and @I18nUnknownFormat is not @I18nFormat(UNUSED)!";
    // Computing the GLB of @I18nFormat with a value and @I18nFormat(null) should never occur in
    // practice. Skipping this case as it causes an expected crash.
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatUnusedAnno, i18nFormatUnusedAnno), i18nFormatUnusedAnno)
        : "GLB of @I18nFormat(UNUSED) and @I18nFormat(UNUSED) is not @I18nFormat(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatUnusedAnno, I18NINVALIDFORMAT), I18NFORMATBOTTOM)
        : "GLB of @I18nFormat(UNUSED) and @I18nInvalidFormat(null) is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatUnusedAnno, i18nInvalidFormatWithMessage),
            I18NFORMATBOTTOM)
        : "GLB of @I18nFormat(UNUSED) and @I18nInvalidFormat(\"Message\") is not"
            + " @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatUnusedAnno, I18NFORMATFOR), I18NFORMATBOTTOM)
        : "GLB of @I18nFormat(UNUSED) and @I18nFormatFor(null) is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatUnusedAnno, i18nFormatForWithValue1), I18NFORMATBOTTOM)
        : "GLB of @I18nFormat(UNUSED) and @I18nFormatFor(\"#1\") is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatUnusedAnno, I18NFORMATBOTTOM), I18NFORMATBOTTOM)
        : "GLB of @I18nFormat(UNUSED) and @I18nFormatBottom is not @I18nFormatBottom!";

    // GLB of @I18nInvalidFormat(null) and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NINVALIDFORMAT, I18NUNKNOWNFORMAT), I18NINVALIDFORMAT)
        : "GLB of @I18nInvalidFormat(null) and @I18nUnknownFormat is not @I18nInvalidFormat(null)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NINVALIDFORMAT, I18NFORMAT), I18NFORMATBOTTOM)
        : "GLB of @I18nInvalidFormat(null) and @I18nFormat(null) is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NINVALIDFORMAT, i18nFormatUnusedAnno), I18NFORMATBOTTOM)
        : "GLB of @I18nInvalidFormat(null) and @I18nFormat(UNUSED) is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NINVALIDFORMAT, I18NFORMATFOR), I18NFORMATBOTTOM)
        : "GLB of @I18nInvalidFormat(null) and @I18nFormatFor(null) is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NINVALIDFORMAT, i18nFormatForWithValue1), I18NFORMATBOTTOM)
        : "GLB of @I18nInvalidFormat(null) and @I18nFormatFor(\"#1\") is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NINVALIDFORMAT, I18NFORMATBOTTOM), I18NFORMATBOTTOM)
        : "GLB of @I18nInvalidFormat(null) and @I18nFormatBottom is not @I18nFormatBottom!";

    // GLB of @I18nInvalidFormat("Message") and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nInvalidFormatWithMessage, I18NUNKNOWNFORMAT),
            i18nInvalidFormatWithMessage)
        : "GLB of @I18nInvalidFormat(\"Message\") and @I18nUnknownFormat is not"
            + " @I18nInvalidFormat(\"Message\")!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nInvalidFormatWithMessage, I18NFORMAT), I18NFORMATBOTTOM)
        : "GLB of @I18nInvalidFormat(\"Message\") and @I18nFormat(null) is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nInvalidFormatWithMessage, i18nFormatUnusedAnno),
            I18NFORMATBOTTOM)
        : "GLB of @I18nInvalidFormat(\"Message\") and @I18nFormat(UNUSED) is not"
            + " @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nInvalidFormatWithMessage, i18nInvalidFormatWithMessage),
            i18nInvalidFormatWithMessage)
        : "GLB of @I18nInvalidFormat(\"Message\") and @I18nInvalidFormat(\"Message\") is not"
            + " @I18nInvalidFormat(\"Message\")!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nInvalidFormatWithMessage, i18nInvalidFormatWithMessage2),
            i18nInvalidFormatWithMessagesAnded)
        : "GLB of @I18nInvalidFormat(\"Message\") and @I18nInvalidFormat(\"Message2\") is not"
            + " @I18nInvalidFormat(\"(\"Message\" and \"Message2\")\")!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nInvalidFormatWithMessage, I18NFORMATFOR), I18NFORMATBOTTOM)
        : "GLB of @I18nInvalidFormat(\"Message\") and @I18nFormatFor(null) is not"
            + " @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nInvalidFormatWithMessage, i18nFormatForWithValue1),
            I18NFORMATBOTTOM)
        : "GLB of @I18nInvalidFormat(\"Message\") and @I18nFormatFor(\"#1\") is not"
            + " @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nInvalidFormatWithMessage, I18NFORMATBOTTOM), I18NFORMATBOTTOM)
        : "GLB of @I18nInvalidFormat(\"Message\") and @I18nFormatBottom is not @I18nFormatBottom!";

    // GLB of @I18nFormatFor(null) and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NFORMATFOR, I18NUNKNOWNFORMAT), I18NFORMATFOR)
        : "GLB of @I18nFormatFor(null) and @I18nUnknownFormat is not @I18nFormatFor(null)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NFORMATFOR, I18NFORMAT), I18NFORMATBOTTOM)
        : "GLB of @I18nFormatFor(null) and @I18nFormat(null) is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NFORMATFOR, i18nFormatUnusedAnno), I18NFORMATBOTTOM)
        : "GLB of @I18nFormatFor(null) and @I18nFormat(UNUSED) is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NFORMATFOR, I18NINVALIDFORMAT), I18NFORMATBOTTOM)
        : "GLB of @I18nFormatFor(null) and @I18nInvalidFormat(null) is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NFORMATFOR, i18nInvalidFormatWithMessage), I18NFORMATBOTTOM)
        : "GLB of @I18nFormatFor(null) and @I18nInvalidFormat(\"Message\") is not"
            + " @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NFORMATFOR, I18NFORMATFOR), I18NFORMATFOR)
        : "GLB of @I18nFormatFor(null) and @I18nFormatFor(null) is not @I18nFormatFor(null)!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NFORMATFOR, i18nFormatForWithValue1), I18NFORMATBOTTOM)
        : "GLB of @I18nFormatFor(null) and @I18nFormatFor(\"#1\") is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NFORMATFOR, I18NFORMATBOTTOM), I18NFORMATBOTTOM)
        : "GLB of @I18nFormatFor(null) and @I18nFormatBottom is not @I18nFormatBottom!";

    // GLB of @I18nFormatFor("#1") and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatForWithValue1, I18NUNKNOWNFORMAT),
            i18nFormatForWithValue1)
        : "GLB of @I18nFormatFor(\"#1\") and @I18nUnknownFormat is not @I18nFormatFor(\"#1\")!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatForWithValue1, I18NFORMAT), I18NFORMATBOTTOM)
        : "GLB of @I18nFormatFor(\"#1\") and @I18nFormat(null) is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatForWithValue1, i18nFormatUnusedAnno), I18NFORMATBOTTOM)
        : "GLB of @I18nFormatFor(\"#1\") and @I18nFormat(UNUSED) is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatForWithValue1, I18NINVALIDFORMAT), I18NFORMATBOTTOM)
        : "GLB of @I18nFormatFor(\"#1\") and @I18nInvalidFormat(null) is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatForWithValue1, i18nInvalidFormatWithMessage),
            I18NFORMATBOTTOM)
        : "GLB of @I18nFormatFor(\"#1\") and @I18nInvalidFormat(\"Message\") is not"
            + " @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatForWithValue1, I18NFORMATFOR), I18NFORMATBOTTOM)
        : "GLB of @I18nFormatFor(\"#1\") and @I18nFormatFor(null) is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatForWithValue1, i18nFormatForWithValue1),
            i18nFormatForWithValue1)
        : "GLB of @I18nFormatFor(\"#1\") and @I18nFormatFor(\"#1\") is not @I18nFormatFor(\"#1\")!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(i18nFormatForWithValue1, I18NFORMATBOTTOM), I18NFORMATBOTTOM)
        : "GLB of @I18nFormatFor(\"#1\") and @I18nFormatBottom is not @I18nFormatBottom!";

    // GLB of @I18nFormatBottom and others

    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NFORMATBOTTOM, I18NUNKNOWNFORMAT), I18NFORMATBOTTOM)
        : "GLB of @I18nFormatBottom and @I18nUnknownFormat is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NFORMATBOTTOM, I18NFORMAT), I18NFORMATBOTTOM)
        : "GLB of @I18nFormatBottom and @I18nFormat(null) is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NFORMATBOTTOM, i18nFormatUnusedAnno), I18NFORMATBOTTOM)
        : "GLB of @I18nFormatBottom and @I18nFormat(UNUSED) is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NFORMATBOTTOM, I18NINVALIDFORMAT), I18NFORMATBOTTOM)
        : "GLB of @I18nFormatBottom and @I18nInvalidFormat(null) is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NFORMATBOTTOM, i18nInvalidFormatWithMessage), I18NFORMATBOTTOM)
        : "GLB of @I18nFormatBottom and @I18nInvalidFormat(\"Message\") is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NFORMATBOTTOM, I18NFORMATFOR), I18NFORMATBOTTOM)
        : "GLB of @I18nFormatBottom and @I18nFormatFor(null) is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NFORMATBOTTOM, i18nFormatForWithValue1), I18NFORMATBOTTOM)
        : "GLB of @I18nFormatBottom and @I18nFormatFor(\"#1\") is not @I18nFormatBottom!";
    assert AnnotationUtils.areSame(
            qh.greatestLowerBound(I18NFORMATBOTTOM, I18NFORMATBOTTOM), I18NFORMATBOTTOM)
        : "GLB of @I18nFormatBottom and @I18nFormatBottom is not @I18nFormatBottom!";

    // ** LUB tests **

    // LUB of UNUSED and others

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatUnusedAnno, i18nFormatUnusedAnno), i18nFormatUnusedAnno)
        : "LUB of @I18nFormat(UNUSED) and @I18nFormat(UNUSED) is not @I18nFormat(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatUnusedAnno, i18nFormatGeneralAnno), i18nFormatGeneralAnno)
        : "LUB of @I18nFormat(UNUSED) and @I18nFormat(GENERAL) is not @I18nFormat(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatUnusedAnno, i18nFormatDateAnno), i18nFormatDateAnno)
        : "LUB of @I18nFormat(UNUSED) and @I18nFormat(DATE) is not @I18nFormat(DATE)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatUnusedAnno, i18nFormatNumberAnno), i18nFormatNumberAnno)
        : "LUB of @I18nFormat(UNUSED) and @I18nFormat(NUMBER) is not @I18nFormat(NUMBER)!";

    // LUB of GENERAL and others

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatGeneralAnno, i18nFormatUnusedAnno), i18nFormatGeneralAnno)
        : "LUB of @I18nFormat(GENERAL) and @I18nFormat(UNUSED) is not @I18nFormat(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatGeneralAnno, i18nFormatGeneralAnno), i18nFormatGeneralAnno)
        : "LUB of @I18nFormat(GENERAL) and @I18nFormat(GENERAL) is not @I18nFormat(GENERAL)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatGeneralAnno, i18nFormatDateAnno), i18nFormatDateAnno)
        : "LUB of @I18nFormat(GENERAL) and @I18nFormat(DATE) is not @I18nFormat(DATE)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatGeneralAnno, i18nFormatNumberAnno), i18nFormatNumberAnno)
        : "LUB of @I18nFormat(GENERAL) and @I18nFormat(NUMBER) is not @I18nFormat(NUMBER)!";

    // LUB of DATE and others

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatDateAnno, i18nFormatUnusedAnno), i18nFormatDateAnno)
        : "LUB of @I18nFormat(DATE) and @I18nFormat(UNUSED) is not @I18nFormat(DATE)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatDateAnno, i18nFormatGeneralAnno), i18nFormatDateAnno)
        : "LUB of @I18nFormat(DATE) and @I18nFormat(GENERAL) is not @I18nFormat(DATE)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatDateAnno, i18nFormatDateAnno), i18nFormatDateAnno)
        : "LUB of @I18nFormat(DATE) and @I18nFormat(DATE) is not @I18nFormat(DATE)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatDateAnno, i18nFormatNumberAnno), i18nFormatNumberAnno)
        : "LUB of @I18nFormat(DATE) and @I18nFormat(NUMBER) is not @I18nFormat(NUMBER)!";

    // LUB of NUMBER and others

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatNumberAnno, i18nFormatUnusedAnno), i18nFormatNumberAnno)
        : "LUB of @I18nFormat(NUMBER) and @I18nFormat(UNUSED) is not @I18nFormat(NUMBER)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatNumberAnno, i18nFormatGeneralAnno), i18nFormatNumberAnno)
        : "LUB of @I18nFormat(NUMBER) and @I18nFormat(GENERAL) is not @I18nFormat(NUMBER)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatNumberAnno, i18nFormatDateAnno), i18nFormatNumberAnno)
        : "LUB of @I18nFormat(NUMBER) and @I18nFormat(DATE) is not @I18nFormat(NUMBER)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatNumberAnno, i18nFormatNumberAnno), i18nFormatNumberAnno)
        : "LUB of @I18nFormat(NUMBER) and @I18nFormat(NUMBER) is not @I18nFormat(NUMBER)!";

    // Now test with two I18nConversionCategory at a time:

    cc2[0] = I18nConversionCategory.DATE;
    cc2[1] = I18nConversionCategory.NUMBER;
    AnnotationMirror formatTwoConvCat4 = treeUtil.categoriesToFormatAnnotation(cc2);

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatTwoConvCat1, formatTwoConvCat2), formatTwoConvCat4)
        : "LUB of @I18nFormat([DATE,DATE]) and @I18nFormat([UNUSED,NUMBER]) is not"
            + " @I18nFormat([DATE,NUMBER])!";

    // Test that the LUB of two I18nConversionCategory arrays of different sizes is an array of
    // the largest size of the two:

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatGeneralAnno, formatTwoConvCat1), formatTwoConvCat1)
        : "LUB of @I18nFormat(GENERAL) and @I18nFormat([DATE,DATE]) is not"
            + " @I18nFormat([DATE,DATE])!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(formatTwoConvCat2, i18nFormatDateAnno), formatTwoConvCat4)
        : "LUB of @I18nFormat([UNUSED,NUMBER]) and @I18nFormat(DATE) is not"
            + " @I18nFormat([DATE,NUMBER])!";

    // LUB of two distinct @I18nFormatFor(...) annotations is @I18nUnknownFormat

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatForWithValue1, i18nFormatForWithValue2), I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormatFor(\"#1\") and @I18nFormatFor(\"#2\") is not @I18nUnknownFormat!";

    // LUB of @I18nUnknownFormat and others

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NUNKNOWNFORMAT, I18NUNKNOWNFORMAT), I18NUNKNOWNFORMAT)
        : "LUB of @I18nUnknownFormat and @I18nUnknownFormat is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NUNKNOWNFORMAT, I18NFORMAT), I18NUNKNOWNFORMAT)
        : "LUB of @I18nUnknownFormat and @I18nFormat(null) is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NUNKNOWNFORMAT, i18nFormatUnusedAnno), I18NUNKNOWNFORMAT)
        : "LUB of @I18nUnknownFormat and @I18nFormat(UNUSED) is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NUNKNOWNFORMAT, I18NINVALIDFORMAT), I18NUNKNOWNFORMAT)
        : "LUB of @I18nUnknownFormat and @I18nInvalidFormat(null) is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NUNKNOWNFORMAT, i18nInvalidFormatWithMessage), I18NUNKNOWNFORMAT)
        : "LUB of @I18nUnknownFormat and @I18nInvalidFormat(\"Message\") is not"
            + " @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NUNKNOWNFORMAT, I18NFORMATFOR), I18NUNKNOWNFORMAT)
        : "LUB of @I18nUnknownFormat and @I18nFormatFor(null) is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NUNKNOWNFORMAT, i18nFormatForWithValue1), I18NUNKNOWNFORMAT)
        : "LUB of @I18nUnknownFormat and @I18nFormatFor(\"#1\") is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NUNKNOWNFORMAT, I18NFORMATBOTTOM), I18NUNKNOWNFORMAT)
        : "LUB of @I18nUnknownFormat and @I18nFormatBottom is not @I18nUnknownFormat!";

    // LUB of @I18nFormat(null) and others

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NFORMAT, I18NUNKNOWNFORMAT), I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormat(null) and @I18nUnknownFormat is not @I18nUnknownFormat!";
    // Computing the LUB of @I18nFormat(null) and @I18nFormat(null) should never occur in
    // practice. Skipping this case as it causes an expected crash.
    // Computing the LUB of @I18nFormat(null) and @I18nFormat with a value should never occur in
    // practice. Skipping this case as it causes an expected crash.
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NFORMAT, I18NINVALIDFORMAT), I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormat(null) and @I18nInvalidFormat(null) is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NFORMAT, i18nInvalidFormatWithMessage), I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormat(null) and @I18nInvalidFormat(\"Message\") is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(qh.leastUpperBound(I18NFORMAT, I18NFORMATFOR), I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormat(null) and @I18nFormatFor(null) is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NFORMAT, i18nFormatForWithValue1), I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormat(null) and @I18nFormatFor(\"#1\") is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(qh.leastUpperBound(I18NFORMAT, I18NFORMATBOTTOM), I18NFORMAT)
        : "LUB of @I18nFormat(null) and @I18nFormatBottom is not @I18nFormat(null)!";

    // LUB of @I18nFormat(UNUSED) and others

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatUnusedAnno, I18NUNKNOWNFORMAT), I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormat(UNUSED) and @I18nUnknownFormat is not @I18nUnknownFormat!";
    // Computing the LUB of @I18nFormat with a value and @I18nFormat(null) should never occur in
    // practice. Skipping this case as it causes an expected crash.
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatUnusedAnno, i18nFormatUnusedAnno), i18nFormatUnusedAnno)
        : "LUB of @I18nFormat(UNUSED) and @I18nFormat(UNUSED) is not @I18nFormat(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatUnusedAnno, I18NINVALIDFORMAT), I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormat(UNUSED) and @I18nInvalidFormat(null) is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatUnusedAnno, i18nInvalidFormatWithMessage),
            I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormat(UNUSED) and @I18nInvalidFormat(\"Message\") is not"
            + " @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatUnusedAnno, I18NFORMATFOR), I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormat(UNUSED) and @I18nFormatFor(null) is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatUnusedAnno, i18nFormatForWithValue1), I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormat(UNUSED) and @I18nFormatFor(\"#1\") is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatUnusedAnno, I18NFORMATBOTTOM), i18nFormatUnusedAnno)
        : "LUB of @I18nFormat(UNUSED) and @I18nFormatBottom is not @I18nFormat(UNUSED)!";

    // LUB of @I18nInvalidFormat(null) and others

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NINVALIDFORMAT, I18NUNKNOWNFORMAT), I18NUNKNOWNFORMAT)
        : "LUB of @I18nInvalidFormat(null) and @I18nUnknownFormat is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NINVALIDFORMAT, I18NFORMAT), I18NUNKNOWNFORMAT)
        : "LUB of @I18nInvalidFormat(null) and @I18nFormat(null) is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NINVALIDFORMAT, i18nFormatUnusedAnno), I18NUNKNOWNFORMAT)
        : "LUB of @I18nInvalidFormat(null) and @I18nFormat(UNUSED) is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NINVALIDFORMAT, I18NFORMATFOR), I18NUNKNOWNFORMAT)
        : "LUB of @I18nInvalidFormat(null) and @I18nFormatFor(null) is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NINVALIDFORMAT, i18nFormatForWithValue1), I18NUNKNOWNFORMAT)
        : "LUB of @I18nInvalidFormat(null) and @I18nFormatFor(\"#1\") is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NINVALIDFORMAT, I18NFORMATBOTTOM), I18NINVALIDFORMAT)
        : "LUB of @I18nInvalidFormat(null) and @I18nFormatBottom is not @I18nInvalidFormat(null)!";

    // LUB of @I18nInvalidFormat("Message") and others

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nInvalidFormatWithMessage, I18NUNKNOWNFORMAT), I18NUNKNOWNFORMAT)
        : "LUB of @I18nInvalidFormat(\"Message\") and @I18nUnknownFormat is not"
            + " @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nInvalidFormatWithMessage, I18NFORMAT), I18NUNKNOWNFORMAT)
        : "LUB of @I18nInvalidFormat(\"Message\") and @I18nFormat(null) is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nInvalidFormatWithMessage, i18nFormatUnusedAnno),
            I18NUNKNOWNFORMAT)
        : "LUB of @I18nInvalidFormat(\"Message\") and @I18nFormat(UNUSED) is not"
            + " @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nInvalidFormatWithMessage, i18nInvalidFormatWithMessage),
            i18nInvalidFormatWithMessage)
        : "LUB of @I18nInvalidFormat(\"Message\") and @I18nInvalidFormat(\"Message\") is not"
            + " @I18nInvalidFormat(\"Message\")!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nInvalidFormatWithMessage, i18nInvalidFormatWithMessage2),
            i18nInvalidFormatWithMessagesOred)
        : "LUB of @I18nInvalidFormat(\"Message\") and @I18nInvalidFormat(\"Message2\") is not"
            + " @I18nInvalidFormat(\"(\"Message\" or \"Message2\")\")!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nInvalidFormatWithMessage, I18NFORMATFOR), I18NUNKNOWNFORMAT)
        : "LUB of @I18nInvalidFormat(\"Message\") and @I18nFormatFor(null) is not"
            + " @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nInvalidFormatWithMessage, i18nFormatForWithValue1),
            I18NUNKNOWNFORMAT)
        : "LUB of @I18nInvalidFormat(\"Message\") and @I18nFormatFor(\"#1\") is not"
            + " @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nInvalidFormatWithMessage, I18NFORMATBOTTOM),
            i18nInvalidFormatWithMessage)
        : "LUB of @I18nInvalidFormat(\"Message\") and @I18nFormatBottom is not"
            + " @I18nInvalidFormat(\"Message\")!";

    // LUB of @I18nFormatFor(null) and others

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NFORMATFOR, I18NUNKNOWNFORMAT), I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormatFor(null) and @I18nUnknownFormat is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(qh.leastUpperBound(I18NFORMATFOR, I18NFORMAT), I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormatFor(null) and @I18nFormat(null) is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NFORMATFOR, i18nFormatUnusedAnno), I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormatFor(null) and @I18nFormat(UNUSED) is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NFORMATFOR, I18NINVALIDFORMAT), I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormatFor(null) and @I18nInvalidFormat(null) is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NFORMATFOR, i18nInvalidFormatWithMessage), I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormatFor(null) and @I18nInvalidFormat(\"Message\") is not"
            + " @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(qh.leastUpperBound(I18NFORMATFOR, I18NFORMATFOR), I18NFORMATFOR)
        : "LUB of @I18nFormatFor(null) and @I18nFormatFor(null) is not @I18nFormatFor(null)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NFORMATFOR, i18nFormatForWithValue1), I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormatFor(null) and @I18nFormatFor(\"#1\") is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NFORMATFOR, I18NFORMATBOTTOM), I18NFORMATFOR)
        : "LUB of @I18nFormatFor(null) and @I18nFormatBottom is not @I18nFormatFor(null)!";

    // LUB of @I18nFormatFor("#1") and others

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatForWithValue1, I18NUNKNOWNFORMAT), I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormatFor(\"#1\") and @I18nUnknownFormat is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatForWithValue1, I18NFORMAT), I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormatFor(\"#1\") and @I18nFormat(null) is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatForWithValue1, i18nFormatUnusedAnno), I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormatFor(\"#1\") and @I18nFormat(UNUSED) is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatForWithValue1, I18NINVALIDFORMAT), I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormatFor(\"#1\") and @I18nInvalidFormat(null) is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatForWithValue1, i18nInvalidFormatWithMessage),
            I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormatFor(\"#1\") and @I18nInvalidFormat(\"Message\") is not"
            + " @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatForWithValue1, I18NFORMATFOR), I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormatFor(\"#1\") and @I18nFormatFor(null) is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatForWithValue1, i18nFormatForWithValue1),
            i18nFormatForWithValue1)
        : "LUB of @I18nFormatFor(\"#1\") and @I18nFormatFor(\"#1\") is not @I18nFormatFor(\"#1\")!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(i18nFormatForWithValue1, I18NFORMATBOTTOM), i18nFormatForWithValue1)
        : "LUB of @I18nFormatFor(\"#1\") and @I18nFormatBottom is not @I18nFormatFor(\"#1\")!";

    // LUB of @I18nFormatBottom and others

    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NFORMATBOTTOM, I18NUNKNOWNFORMAT), I18NUNKNOWNFORMAT)
        : "LUB of @I18nFormatBottom and @I18nUnknownFormat is not @I18nUnknownFormat!";
    assert AnnotationUtils.areSame(qh.leastUpperBound(I18NFORMATBOTTOM, I18NFORMAT), I18NFORMAT)
        : "LUB of @I18nFormatBottom and @I18nFormat(null) is not @I18nFormat(null)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NFORMATBOTTOM, i18nFormatUnusedAnno), i18nFormatUnusedAnno)
        : "LUB of @I18nFormatBottom and @I18nFormat(UNUSED) is not @I18nFormat(UNUSED)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NFORMATBOTTOM, I18NINVALIDFORMAT), I18NINVALIDFORMAT)
        : "LUB of @I18nFormatBottom and @I18nInvalidFormat(null) is not @I18nInvalidFormat(null)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NFORMATBOTTOM, i18nInvalidFormatWithMessage),
            i18nInvalidFormatWithMessage)
        : "LUB of @I18nFormatBottom and @I18nInvalidFormat(\"Message\") is not"
            + " @I18nInvalidFormat(\"Message\")!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NFORMATBOTTOM, I18NFORMATFOR), I18NFORMATFOR)
        : "LUB of @I18nFormatBottom and @I18nFormatFor(null) is not @I18nFormatFor(null)!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NFORMATBOTTOM, i18nFormatForWithValue1), i18nFormatForWithValue1)
        : "LUB of @I18nFormatBottom and @I18nFormatFor(\"#1\") is not @I18nFormatFor(\"#1\")!";
    assert AnnotationUtils.areSame(
            qh.leastUpperBound(I18NFORMATBOTTOM, I18NFORMATBOTTOM), I18NFORMATBOTTOM)
        : "LUB of @I18nFormatBottom and @I18nFormatBottom is not @I18nFormatBottom!";
  }
}
