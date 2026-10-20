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

    glbAssert(i18nFormatUnusedAnno, i18nFormatUnusedAnno, i18nFormatUnusedAnno);
    glbAssert(i18nFormatUnusedAnno, i18nFormatGeneralAnno, i18nFormatUnusedAnno);
    glbAssert(i18nFormatUnusedAnno, i18nFormatDateAnno, i18nFormatUnusedAnno);
    glbAssert(i18nFormatUnusedAnno, i18nFormatNumberAnno, i18nFormatUnusedAnno);

    // GLB of GENERAL and others

    glbAssert(i18nFormatGeneralAnno, i18nFormatUnusedAnno, i18nFormatUnusedAnno);
    glbAssert(i18nFormatGeneralAnno, i18nFormatGeneralAnno, i18nFormatGeneralAnno);
    glbAssert(i18nFormatGeneralAnno, i18nFormatDateAnno, i18nFormatGeneralAnno);
    glbAssert(i18nFormatGeneralAnno, i18nFormatNumberAnno, i18nFormatGeneralAnno);

    // GLB of DATE and others

    glbAssert(i18nFormatDateAnno, i18nFormatUnusedAnno, i18nFormatUnusedAnno);
    glbAssert(i18nFormatDateAnno, i18nFormatGeneralAnno, i18nFormatGeneralAnno);
    glbAssert(i18nFormatDateAnno, i18nFormatDateAnno, i18nFormatDateAnno);
    glbAssert(i18nFormatDateAnno, i18nFormatNumberAnno, i18nFormatDateAnno);

    // GLB of NUMBER and others

    glbAssert(i18nFormatNumberAnno, i18nFormatUnusedAnno, i18nFormatUnusedAnno);
    glbAssert(i18nFormatNumberAnno, i18nFormatGeneralAnno, i18nFormatGeneralAnno);
    glbAssert(i18nFormatNumberAnno, i18nFormatDateAnno, i18nFormatDateAnno);
    glbAssert(i18nFormatNumberAnno, i18nFormatNumberAnno, i18nFormatNumberAnno);

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

    glbAssert(formatTwoConvCat1, formatTwoConvCat2, formatTwoConvCat3);

    // Test that the GLB of two I18nConversionCategory arrays of different sizes is an array of
    // the smallest size of the two:

    glbAssert(i18nFormatGeneralAnno, formatTwoConvCat1, i18nFormatGeneralAnno);
    glbAssert(formatTwoConvCat2, i18nFormatDateAnno, i18nFormatUnusedAnno);

    // GLB of two distinct @I18nFormatFor(...) annotations is @I18nFormatBottom

    glbAssert(i18nFormatForWithValue1, i18nFormatForWithValue2, I18NFORMATBOTTOM);

    // GLB of @I18nUnknownFormat and others

    glbAssert(I18NUNKNOWNFORMAT, I18NUNKNOWNFORMAT, I18NUNKNOWNFORMAT);
    glbAssert(I18NUNKNOWNFORMAT, I18NFORMAT, I18NFORMAT);
    glbAssert(I18NUNKNOWNFORMAT, i18nFormatUnusedAnno, i18nFormatUnusedAnno);
    glbAssert(I18NUNKNOWNFORMAT, I18NINVALIDFORMAT, I18NINVALIDFORMAT);
    glbAssert(I18NUNKNOWNFORMAT, i18nInvalidFormatWithMessage, i18nInvalidFormatWithMessage);
    glbAssert(I18NUNKNOWNFORMAT, I18NFORMATFOR, I18NFORMATFOR);
    glbAssert(I18NUNKNOWNFORMAT, i18nFormatForWithValue1, i18nFormatForWithValue1);
    glbAssert(I18NUNKNOWNFORMAT, I18NFORMATBOTTOM, I18NFORMATBOTTOM);

    // GLB of @I18nFormat(null) and others

    glbAssert(I18NFORMAT, I18NUNKNOWNFORMAT, I18NFORMAT);
    // Computing the GLB of @I18nFormat(null) and @I18nFormat(null) should never occur in
    // practice. Skipping this case as it causes an expected crash.
    // Computing the GLB of @I18nFormat(null) and @I18nFormat with a value should never occur in
    // practice. Skipping this case as it causes an expected crash.
    glbAssert(I18NFORMAT, I18NINVALIDFORMAT, I18NFORMATBOTTOM);
    glbAssert(I18NFORMAT, i18nInvalidFormatWithMessage, I18NFORMATBOTTOM);
    glbAssert(I18NFORMAT, I18NFORMATFOR, I18NFORMATBOTTOM);
    glbAssert(I18NFORMAT, i18nFormatForWithValue1, I18NFORMATBOTTOM);
    glbAssert(I18NFORMAT, I18NFORMATBOTTOM, I18NFORMATBOTTOM);

    // GLB of @I18nFormat(UNUSED) and others

    glbAssert(i18nFormatUnusedAnno, I18NUNKNOWNFORMAT, i18nFormatUnusedAnno);
    // Computing the GLB of @I18nFormat with a value and @I18nFormat(null) should never occur in
    // practice. Skipping this case as it causes an expected crash.
    glbAssert(i18nFormatUnusedAnno, i18nFormatUnusedAnno, i18nFormatUnusedAnno);
    glbAssert(i18nFormatUnusedAnno, I18NINVALIDFORMAT, I18NFORMATBOTTOM);
    glbAssert(i18nFormatUnusedAnno, i18nInvalidFormatWithMessage, I18NFORMATBOTTOM);
    glbAssert(i18nFormatUnusedAnno, I18NFORMATFOR, I18NFORMATBOTTOM);
    glbAssert(i18nFormatUnusedAnno, i18nFormatForWithValue1, I18NFORMATBOTTOM);
    glbAssert(i18nFormatUnusedAnno, I18NFORMATBOTTOM, I18NFORMATBOTTOM);

    // GLB of @I18nInvalidFormat(null) and others

    glbAssert(I18NINVALIDFORMAT, I18NUNKNOWNFORMAT, I18NINVALIDFORMAT);
    glbAssert(I18NINVALIDFORMAT, I18NFORMAT, I18NFORMATBOTTOM);
    glbAssert(I18NINVALIDFORMAT, i18nFormatUnusedAnno, I18NFORMATBOTTOM);
    glbAssert(I18NINVALIDFORMAT, I18NFORMATFOR, I18NFORMATBOTTOM);
    glbAssert(I18NINVALIDFORMAT, i18nFormatForWithValue1, I18NFORMATBOTTOM);
    glbAssert(I18NINVALIDFORMAT, I18NFORMATBOTTOM, I18NFORMATBOTTOM);

    // GLB of @I18nInvalidFormat("Message") and others

    glbAssert(i18nInvalidFormatWithMessage, I18NUNKNOWNFORMAT, i18nInvalidFormatWithMessage);

    glbAssert(i18nInvalidFormatWithMessage, I18NFORMAT, I18NFORMATBOTTOM);
    glbAssert(i18nInvalidFormatWithMessage, i18nFormatUnusedAnno, I18NFORMATBOTTOM);
    glbAssert(
        i18nInvalidFormatWithMessage, i18nInvalidFormatWithMessage, i18nInvalidFormatWithMessage);
    glbAssert(
        i18nInvalidFormatWithMessage,
        i18nInvalidFormatWithMessage2,
        i18nInvalidFormatWithMessagesAnded);
    glbAssert(i18nInvalidFormatWithMessage, I18NFORMATFOR, I18NFORMATBOTTOM);
    glbAssert(i18nInvalidFormatWithMessage, i18nFormatForWithValue1, I18NFORMATBOTTOM);
    glbAssert(i18nInvalidFormatWithMessage, I18NFORMATBOTTOM, I18NFORMATBOTTOM);

    // GLB of @I18nFormatFor(null) and others

    glbAssert(I18NFORMATFOR, I18NUNKNOWNFORMAT, I18NFORMATFOR);
    glbAssert(I18NFORMATFOR, I18NFORMAT, I18NFORMATBOTTOM);
    glbAssert(I18NFORMATFOR, i18nFormatUnusedAnno, I18NFORMATBOTTOM);
    glbAssert(I18NFORMATFOR, I18NINVALIDFORMAT, I18NFORMATBOTTOM);
    glbAssert(I18NFORMATFOR, i18nInvalidFormatWithMessage, I18NFORMATBOTTOM);
    glbAssert(I18NFORMATFOR, I18NFORMATFOR, I18NFORMATFOR);
    glbAssert(I18NFORMATFOR, i18nFormatForWithValue1, I18NFORMATBOTTOM);
    glbAssert(I18NFORMATFOR, I18NFORMATBOTTOM, I18NFORMATBOTTOM);

    // GLB of @I18nFormatFor("#1") and others

    glbAssert(i18nFormatForWithValue1, I18NUNKNOWNFORMAT, i18nFormatForWithValue1);
    glbAssert(i18nFormatForWithValue1, I18NFORMAT, I18NFORMATBOTTOM);
    glbAssert(i18nFormatForWithValue1, i18nFormatUnusedAnno, I18NFORMATBOTTOM);
    glbAssert(i18nFormatForWithValue1, I18NINVALIDFORMAT, I18NFORMATBOTTOM);
    glbAssert(i18nFormatForWithValue1, i18nInvalidFormatWithMessage, I18NFORMATBOTTOM);
    glbAssert(i18nFormatForWithValue1, I18NFORMATFOR, I18NFORMATBOTTOM);
    glbAssert(i18nFormatForWithValue1, i18nFormatForWithValue1, i18nFormatForWithValue1);
    glbAssert(i18nFormatForWithValue1, I18NFORMATBOTTOM, I18NFORMATBOTTOM);

    // GLB of @I18nFormatBottom and others

    glbAssert(I18NFORMATBOTTOM, I18NUNKNOWNFORMAT, I18NFORMATBOTTOM);
    glbAssert(I18NFORMATBOTTOM, I18NFORMAT, I18NFORMATBOTTOM);
    glbAssert(I18NFORMATBOTTOM, i18nFormatUnusedAnno, I18NFORMATBOTTOM);
    glbAssert(I18NFORMATBOTTOM, I18NINVALIDFORMAT, I18NFORMATBOTTOM);
    glbAssert(I18NFORMATBOTTOM, i18nInvalidFormatWithMessage, I18NFORMATBOTTOM);
    glbAssert(I18NFORMATBOTTOM, I18NFORMATFOR, I18NFORMATBOTTOM);
    glbAssert(I18NFORMATBOTTOM, i18nFormatForWithValue1, I18NFORMATBOTTOM);
    glbAssert(I18NFORMATBOTTOM, I18NFORMATBOTTOM, I18NFORMATBOTTOM);

    // ** LUB tests **

    // LUB of UNUSED and others

    lubAssert(i18nFormatUnusedAnno, i18nFormatUnusedAnno, i18nFormatUnusedAnno);
    lubAssert(i18nFormatUnusedAnno, i18nFormatGeneralAnno, i18nFormatGeneralAnno);
    lubAssert(i18nFormatUnusedAnno, i18nFormatDateAnno, i18nFormatDateAnno);
    lubAssert(i18nFormatUnusedAnno, i18nFormatNumberAnno, i18nFormatNumberAnno);

    // LUB of GENERAL and others

    lubAssert(i18nFormatGeneralAnno, i18nFormatUnusedAnno, i18nFormatGeneralAnno);
    lubAssert(i18nFormatGeneralAnno, i18nFormatGeneralAnno, i18nFormatGeneralAnno);
    lubAssert(i18nFormatGeneralAnno, i18nFormatDateAnno, i18nFormatDateAnno);
    lubAssert(i18nFormatGeneralAnno, i18nFormatNumberAnno, i18nFormatNumberAnno);

    // LUB of DATE and others

    lubAssert(i18nFormatDateAnno, i18nFormatUnusedAnno, i18nFormatDateAnno);
    lubAssert(i18nFormatDateAnno, i18nFormatGeneralAnno, i18nFormatDateAnno);
    lubAssert(i18nFormatDateAnno, i18nFormatDateAnno, i18nFormatDateAnno);
    lubAssert(i18nFormatDateAnno, i18nFormatNumberAnno, i18nFormatNumberAnno);

    // LUB of NUMBER and others

    lubAssert(i18nFormatNumberAnno, i18nFormatUnusedAnno, i18nFormatNumberAnno);
    lubAssert(i18nFormatNumberAnno, i18nFormatGeneralAnno, i18nFormatNumberAnno);
    lubAssert(i18nFormatNumberAnno, i18nFormatDateAnno, i18nFormatNumberAnno);
    lubAssert(i18nFormatNumberAnno, i18nFormatNumberAnno, i18nFormatNumberAnno);

    // Now test with two I18nConversionCategory at a time:

    cc2[0] = I18nConversionCategory.DATE;
    cc2[1] = I18nConversionCategory.NUMBER;
    AnnotationMirror formatTwoConvCat4 = treeUtil.categoriesToFormatAnnotation(cc2);

    lubAssert(formatTwoConvCat1, formatTwoConvCat2, formatTwoConvCat4);

    // Test that the LUB of two I18nConversionCategory arrays of different sizes is an array of
    // the largest size of the two:

    lubAssert(i18nFormatGeneralAnno, formatTwoConvCat1, formatTwoConvCat1);
    lubAssert(formatTwoConvCat2, i18nFormatDateAnno, formatTwoConvCat4);

    // LUB of two distinct @I18nFormatFor(...) annotations is @I18nUnknownFormat

    lubAssert(i18nFormatForWithValue1, i18nFormatForWithValue2, I18NUNKNOWNFORMAT);

    // LUB of @I18nUnknownFormat and others

    lubAssert(I18NUNKNOWNFORMAT, I18NUNKNOWNFORMAT, I18NUNKNOWNFORMAT);
    lubAssert(I18NUNKNOWNFORMAT, I18NFORMAT, I18NUNKNOWNFORMAT);
    lubAssert(I18NUNKNOWNFORMAT, i18nFormatUnusedAnno, I18NUNKNOWNFORMAT);
    lubAssert(I18NUNKNOWNFORMAT, I18NINVALIDFORMAT, I18NUNKNOWNFORMAT);
    lubAssert(I18NUNKNOWNFORMAT, i18nInvalidFormatWithMessage, I18NUNKNOWNFORMAT);
    lubAssert(I18NUNKNOWNFORMAT, I18NFORMATFOR, I18NUNKNOWNFORMAT);
    lubAssert(I18NUNKNOWNFORMAT, i18nFormatForWithValue1, I18NUNKNOWNFORMAT);
    lubAssert(I18NUNKNOWNFORMAT, I18NFORMATBOTTOM, I18NUNKNOWNFORMAT);

    // LUB of @I18nFormat(null) and others

    lubAssert(I18NFORMAT, I18NUNKNOWNFORMAT, I18NUNKNOWNFORMAT);
    // Computing the LUB of @I18nFormat(null) and @I18nFormat(null) should never occur in
    // practice. Skipping this case as it causes an expected crash.
    // Computing the LUB of @I18nFormat(null) and @I18nFormat with a value should never occur in
    // practice. Skipping this case as it causes an expected crash.
    lubAssert(I18NFORMAT, I18NINVALIDFORMAT, I18NUNKNOWNFORMAT);
    lubAssert(I18NFORMAT, i18nInvalidFormatWithMessage, I18NUNKNOWNFORMAT);
    lubAssert(I18NFORMAT, I18NFORMATFOR, I18NUNKNOWNFORMAT);
    lubAssert(I18NFORMAT, i18nFormatForWithValue1, I18NUNKNOWNFORMAT);
    lubAssert(I18NFORMAT, I18NFORMATBOTTOM, I18NFORMAT);

    // LUB of @I18nFormat(UNUSED) and others

    lubAssert(i18nFormatUnusedAnno, I18NUNKNOWNFORMAT, I18NUNKNOWNFORMAT);
    // Computing the LUB of @I18nFormat with a value and @I18nFormat(null) should never occur in
    // practice. Skipping this case as it causes an expected crash.
    lubAssert(i18nFormatUnusedAnno, i18nFormatUnusedAnno, i18nFormatUnusedAnno);
    lubAssert(i18nFormatUnusedAnno, I18NINVALIDFORMAT, I18NUNKNOWNFORMAT);
    lubAssert(i18nFormatUnusedAnno, i18nInvalidFormatWithMessage, I18NUNKNOWNFORMAT);
    lubAssert(i18nFormatUnusedAnno, I18NFORMATFOR, I18NUNKNOWNFORMAT);
    lubAssert(i18nFormatUnusedAnno, i18nFormatForWithValue1, I18NUNKNOWNFORMAT);
    lubAssert(i18nFormatUnusedAnno, I18NFORMATBOTTOM, i18nFormatUnusedAnno);

    // LUB of @I18nInvalidFormat(null) and others

    lubAssert(I18NINVALIDFORMAT, I18NUNKNOWNFORMAT, I18NUNKNOWNFORMAT);
    lubAssert(I18NINVALIDFORMAT, I18NFORMAT, I18NUNKNOWNFORMAT);
    lubAssert(I18NINVALIDFORMAT, i18nFormatUnusedAnno, I18NUNKNOWNFORMAT);
    lubAssert(I18NINVALIDFORMAT, I18NFORMATFOR, I18NUNKNOWNFORMAT);
    lubAssert(I18NINVALIDFORMAT, i18nFormatForWithValue1, I18NUNKNOWNFORMAT);
    lubAssert(I18NINVALIDFORMAT, I18NFORMATBOTTOM, I18NINVALIDFORMAT);

    // LUB of @I18nInvalidFormat("Message") and others

    lubAssert(i18nInvalidFormatWithMessage, I18NUNKNOWNFORMAT, I18NUNKNOWNFORMAT);

    lubAssert(i18nInvalidFormatWithMessage, I18NFORMAT, I18NUNKNOWNFORMAT);
    lubAssert(i18nInvalidFormatWithMessage, i18nFormatUnusedAnno, I18NUNKNOWNFORMAT);
    lubAssert(
        i18nInvalidFormatWithMessage, i18nInvalidFormatWithMessage, i18nInvalidFormatWithMessage);
    lubAssert(
        i18nInvalidFormatWithMessage,
        i18nInvalidFormatWithMessage2,
        i18nInvalidFormatWithMessagesOred);
    lubAssert(i18nInvalidFormatWithMessage, I18NFORMATFOR, I18NUNKNOWNFORMAT);
    lubAssert(i18nInvalidFormatWithMessage, i18nFormatForWithValue1, I18NUNKNOWNFORMAT);
    lubAssert(i18nInvalidFormatWithMessage, I18NFORMATBOTTOM, i18nInvalidFormatWithMessage);

    // LUB of @I18nFormatFor(null) and others

    lubAssert(I18NFORMATFOR, I18NUNKNOWNFORMAT, I18NUNKNOWNFORMAT);
    lubAssert(I18NFORMATFOR, I18NFORMAT, I18NUNKNOWNFORMAT);
    lubAssert(I18NFORMATFOR, i18nFormatUnusedAnno, I18NUNKNOWNFORMAT);
    lubAssert(I18NFORMATFOR, I18NINVALIDFORMAT, I18NUNKNOWNFORMAT);
    lubAssert(I18NFORMATFOR, i18nInvalidFormatWithMessage, I18NUNKNOWNFORMAT);
    lubAssert(I18NFORMATFOR, I18NFORMATFOR, I18NFORMATFOR);
    lubAssert(I18NFORMATFOR, i18nFormatForWithValue1, I18NUNKNOWNFORMAT);
    lubAssert(I18NFORMATFOR, I18NFORMATBOTTOM, I18NFORMATFOR);

    // LUB of @I18nFormatFor("#1") and others

    lubAssert(i18nFormatForWithValue1, I18NUNKNOWNFORMAT, I18NUNKNOWNFORMAT);
    lubAssert(i18nFormatForWithValue1, I18NFORMAT, I18NUNKNOWNFORMAT);
    lubAssert(i18nFormatForWithValue1, i18nFormatUnusedAnno, I18NUNKNOWNFORMAT);
    lubAssert(i18nFormatForWithValue1, I18NINVALIDFORMAT, I18NUNKNOWNFORMAT);
    lubAssert(i18nFormatForWithValue1, i18nInvalidFormatWithMessage, I18NUNKNOWNFORMAT);
    lubAssert(i18nFormatForWithValue1, I18NFORMATFOR, I18NUNKNOWNFORMAT);
    lubAssert(i18nFormatForWithValue1, i18nFormatForWithValue1, i18nFormatForWithValue1);
    lubAssert(i18nFormatForWithValue1, I18NFORMATBOTTOM, i18nFormatForWithValue1);

    // LUB of @I18nFormatBottom and others

    lubAssert(I18NFORMATBOTTOM, I18NUNKNOWNFORMAT, I18NUNKNOWNFORMAT);
    lubAssert(I18NFORMATBOTTOM, I18NFORMAT, I18NFORMAT);
    lubAssert(I18NFORMATBOTTOM, i18nFormatUnusedAnno, i18nFormatUnusedAnno);
    lubAssert(I18NFORMATBOTTOM, I18NINVALIDFORMAT, I18NINVALIDFORMAT);
    lubAssert(I18NFORMATBOTTOM, i18nInvalidFormatWithMessage, i18nInvalidFormatWithMessage);
    lubAssert(I18NFORMATBOTTOM, I18NFORMATFOR, I18NFORMATFOR);
    lubAssert(I18NFORMATBOTTOM, i18nFormatForWithValue1, i18nFormatForWithValue1);
    lubAssert(I18NFORMATBOTTOM, I18NFORMATBOTTOM, I18NFORMATBOTTOM);
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
    AnnotationMirror result = qualHierarchy.greatestLowerBoundQualifiersOnly(arg1, arg2);
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
    AnnotationMirror result = qualHierarchy.leastUpperBoundQualifiersOnly(arg1, arg2);
    if (!AnnotationUtils.areSame(expected, result)) {
      throw new AssertionError(
          String.format("LUB of %s and %s should be %s, but is %s", arg1, arg2, expected, result));
    }
  }
}
