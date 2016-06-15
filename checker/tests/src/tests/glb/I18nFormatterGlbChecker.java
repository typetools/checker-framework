package tests.glb;

// Test case for issue 723.
// https://github.com/typetools/checker-framework/issues/723

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

import org.checkerframework.checker.i18nformatter.I18nFormatterChecker;
import org.checkerframework.checker.i18nformatter.I18nFormatterTreeUtil;
import org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.checkerframework.checker.i18nformatter.qual.I18nFormatBottom;
import org.checkerframework.checker.i18nformatter.qual.I18nFormatFor;
import org.checkerframework.checker.i18nformatter.qual.I18nInvalidFormat;
import org.checkerframework.checker.i18nformatter.qual.I18nUnknownFormat;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * This class tests the implementation of GLB computation in the
 * I18n Format String Checker (see issue 723), but it does not
 * test for the crash that occurs if I18nFormatterAnnotatedTypeFactory
 * does not override greatestLowerBound.
 * That is done by tests/all-systems/Issue691.java.
 */
public class I18nFormatterGlbChecker extends I18nFormatterChecker {

    @Override
    public void initChecker() {
        super.initChecker();
        I18nFormatterTreeUtil treeUtil = new I18nFormatterTreeUtil(this);

      Elements elements = getElementUtils();
      AnnotationMirror I18NUNKNOWNFORMAT = AnnotationUtils.fromClass(elements, I18nUnknownFormat.class);
        AnnotationMirror I18NFORMAT = AnnotationUtils.fromClass(elements, I18nFormat.class);
        AnnotationMirror I18NINVALIDFORMAT = AnnotationUtils.fromClass(elements, I18nInvalidFormat.class);
        AnnotationMirror I18NFORMATFOR = AnnotationUtils.fromClass(elements, I18nFormatFor.class);
        AnnotationMirror I18NFORMATBOTTOM = AnnotationUtils.fromClass(elements, I18nFormatBottom.class);

        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, I18nInvalidFormat.class.getCanonicalName());
        builder.setValue("value", "Message");
        AnnotationMirror I18nInvalidFormatWithMessage = builder.build();

        builder = new AnnotationBuilder(processingEnv, I18nFormatFor.class.getCanonicalName());
        builder.setValue("value", "#1");
        AnnotationMirror I18nFormatForWithValue1 = builder.build();

        builder = new AnnotationBuilder(processingEnv, I18nFormatFor.class.getCanonicalName());
        builder.setValue("value", "#2");
        AnnotationMirror I18nFormatForWithValue2 = builder.build();

        I18nConversionCategory[] cc = new I18nConversionCategory[1];

        cc[0] = I18nConversionCategory.UNUSED;
        AnnotationMirror I18nFormatUnusedAnno = treeUtil.categoriesToFormatAnnotation(cc);
        cc[0] = I18nConversionCategory.GENERAL;
        AnnotationMirror I18nFormatGeneralAnno = treeUtil.categoriesToFormatAnnotation(cc);
        cc[0] = I18nConversionCategory.DATE;
        AnnotationMirror I18nFormatDateAnno = treeUtil.categoriesToFormatAnnotation(cc);
        cc[0] = I18nConversionCategory.NUMBER;
        AnnotationMirror I18nFormatNumberAnno = treeUtil.categoriesToFormatAnnotation(cc);

        QualifierHierarchy qh = ((BaseTypeVisitor<?>)visitor).getTypeFactory().getQualifierHierarchy();

        // GLB of UNUSED and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatUnusedAnno, I18nFormatUnusedAnno), I18nFormatUnusedAnno) :
            "GLB of @I18nFormat(UNUSED) and @I18nFormat(UNUSED) is not @I18nFormat(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatUnusedAnno, I18nFormatGeneralAnno), I18nFormatUnusedAnno) :
            "GLB of @I18nFormat(UNUSED) and @I18nFormat(GENERAL) is not @I18nFormat(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatUnusedAnno, I18nFormatDateAnno), I18nFormatUnusedAnno) :
            "GLB of @I18nFormat(UNUSED) and @I18nFormat(DATE) is not @I18nFormat(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatUnusedAnno, I18nFormatNumberAnno), I18nFormatUnusedAnno) :
            "GLB of @I18nFormat(UNUSED) and @I18nFormat(NUMBER) is not @I18nFormat(UNUSED)!";

        // GLB of GENERAL and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatGeneralAnno, I18nFormatUnusedAnno), I18nFormatUnusedAnno) :
            "GLB of @I18nFormat(GENERAL) and @I18nFormat(UNUSED) is not @I18nFormat(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatGeneralAnno, I18nFormatGeneralAnno), I18nFormatGeneralAnno) :
            "GLB of @I18nFormat(GENERAL) and @I18nFormat(GENERAL) is not @I18nFormat(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatGeneralAnno, I18nFormatDateAnno), I18nFormatGeneralAnno) :
            "GLB of @I18nFormat(GENERAL) and @I18nFormat(DATE) is not @I18nFormat(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatGeneralAnno, I18nFormatNumberAnno), I18nFormatGeneralAnno) :
            "GLB of @I18nFormat(GENERAL) and @I18nFormat(NUMBER) is not @I18nFormat(GENERAL)!";

        // GLB of DATE and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatDateAnno, I18nFormatUnusedAnno), I18nFormatUnusedAnno) :
            "GLB of @I18nFormat(DATE) and @I18nFormat(UNUSED) is not @I18nFormat(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatDateAnno, I18nFormatGeneralAnno), I18nFormatGeneralAnno) :
            "GLB of @I18nFormat(DATE) and @I18nFormat(GENERAL) is not @I18nFormat(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatDateAnno, I18nFormatDateAnno), I18nFormatDateAnno) :
            "GLB of @I18nFormat(DATE) and @I18nFormat(DATE) is not @I18nFormat(DATE)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatDateAnno, I18nFormatNumberAnno), I18nFormatDateAnno) :
            "GLB of @I18nFormat(DATE) and @I18nFormat(NUMBER) is not @I18nFormat(DATE)!";

        // GLB of NUMBER and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatNumberAnno, I18nFormatUnusedAnno), I18nFormatUnusedAnno) :
            "GLB of @I18nFormat(NUMBER) and @I18nFormat(UNUSED) is not @I18nFormat(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatNumberAnno, I18nFormatGeneralAnno), I18nFormatGeneralAnno) :
            "GLB of @I18nFormat(NUMBER) and @I18nFormat(GENERAL) is not @I18nFormat(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatNumberAnno, I18nFormatDateAnno), I18nFormatDateAnno) :
            "GLB of @I18nFormat(NUMBER) and @I18nFormat(DATE) is not @I18nFormat(DATE)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatNumberAnno, I18nFormatNumberAnno), I18nFormatNumberAnno) :
            "GLB of @I18nFormat(NUMBER) and @I18nFormat(NUMBER) is not @I18nFormat(NUMBER)!";

        // Now test with two I18nConversionCategory at a time:

        I18nConversionCategory[] cc2 = new I18nConversionCategory[2];

        cc2[0] = I18nConversionCategory.DATE;
        cc2[1] = I18nConversionCategory.DATE;
        AnnotationMirror FormatTwoConvCat1 = treeUtil.categoriesToFormatAnnotation(cc2);
        cc2[0] = I18nConversionCategory.UNUSED;
        cc2[1] = I18nConversionCategory.NUMBER;
        AnnotationMirror FormatTwoConvCat2 = treeUtil.categoriesToFormatAnnotation(cc2);
        cc2[0] = I18nConversionCategory.UNUSED;
        cc2[1] = I18nConversionCategory.DATE;
        AnnotationMirror FormatTwoConvCat3 = treeUtil.categoriesToFormatAnnotation(cc2);

        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatTwoConvCat1, FormatTwoConvCat2), FormatTwoConvCat3) :
            "GLB of @I18nFormat([DATE,DATE]) and @I18nFormat([UNUSED,NUMBER]) is not @I18nFormat([UNUSED,DATE])!";

        // Test that the GLB of two I18nConversionCategory arrays of different sizes is an array of the smallest size of the two:

        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatGeneralAnno, FormatTwoConvCat1), I18nFormatGeneralAnno) :
            "GLB of @I18nFormat(GENERAL) and @I18nFormat([DATE,DATE]) is not @I18nFormat(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatTwoConvCat2, I18nFormatDateAnno), I18nFormatUnusedAnno) :
            "GLB of @I18nFormat([UNUSED,NUMBER]) and @I18nFormat(DATE) is not @I18nFormat(UNUSED)!";

        // GLB of two distinct @I18nFormatFor(...) annotations is @I18nFormatBottom

        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatForWithValue1, I18nFormatForWithValue2), I18NFORMATBOTTOM) :
            "GLB of @I18nFormatFor(\"#1\") and @I18nFormatFor(\"#2\") is not @I18nFormatBottom!";

        // GLB of @I18nUnknownFormat and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NUNKNOWNFORMAT, I18NUNKNOWNFORMAT), I18NUNKNOWNFORMAT) :
            "GLB of @I18nUnknownFormat and @I18nUnknownFormat is not @I18nUnknownFormat!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NUNKNOWNFORMAT, I18NFORMAT), I18NFORMAT) :
            "GLB of @I18nUnknownFormat and @I18nFormat(null) is not @I18nFormat(null)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NUNKNOWNFORMAT, I18nFormatUnusedAnno), I18nFormatUnusedAnno) :
            "GLB of @I18nUnknownFormat and @I18nFormat(UNUSED) is not @I18nFormat(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NUNKNOWNFORMAT, I18NINVALIDFORMAT), I18NINVALIDFORMAT) :
            "GLB of @I18nUnknownFormat and @I18nInvalidFormat(null) is not @I18nInvalidFormat(null)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NUNKNOWNFORMAT, I18nInvalidFormatWithMessage), I18nInvalidFormatWithMessage) :
            "GLB of @I18nUnknownFormat and @I18nInvalidFormat(\"Message\") is not @I18nInvalidFormat(\"Message\")!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NUNKNOWNFORMAT, I18NFORMATFOR), I18NFORMATFOR) :
            "GLB of @I18nUnknownFormat and @I18nFormatFor(null) is not @I18nFormatFor(null)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NUNKNOWNFORMAT, I18nFormatForWithValue1), I18nFormatForWithValue1) :
            "GLB of @I18nUnknownFormat and @I18nFormatFor(\"#1\") is not @I18nFormatFor(\"#1\")!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NUNKNOWNFORMAT, I18NFORMATBOTTOM), I18NFORMATBOTTOM) :
            "GLB of @I18nUnknownFormat and @I18nFormatBottom is not @I18nFormatBottom!";

        // GLB of @I18nFormat(null) and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NFORMAT, I18NUNKNOWNFORMAT), I18NFORMAT) :
            "GLB of @I18nFormat(null) and @I18nUnknownFormat is not @I18nFormat(null)!";
        // Computing the GLB of @I18nFormat(null) and @I18nFormat(null) should never occur in practice. Skipping this case as it causes an expected crash.
        // Computing the GLB of @I18nFormat(null) and @I18nFormat with a value should never occur in practice. Skipping this case as it causes an expected crash.
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NFORMAT, I18NINVALIDFORMAT), I18NFORMATBOTTOM) :
            "GLB of @I18nFormat(null) and @I18nInvalidFormat(null) is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NFORMAT, I18nInvalidFormatWithMessage), I18NFORMATBOTTOM) :
            "GLB of @I18nFormat(null) and @I18nInvalidFormat(\"Message\") is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NFORMAT, I18NFORMATFOR), I18NFORMATBOTTOM) :
            "GLB of @I18nFormat(null) and @I18nFormatFor(null) is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NFORMAT, I18nFormatForWithValue1), I18NFORMATBOTTOM) :
            "GLB of @I18nFormat(null) and @I18nFormatFor(\"#1\") is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NFORMAT, I18NFORMATBOTTOM), I18NFORMATBOTTOM) :
            "GLB of @I18nFormat(null) and @I18nFormatBottom is not @I18nFormatBottom!";

        // GLB of @I18nFormat(UNUSED) and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatUnusedAnno, I18NUNKNOWNFORMAT), I18nFormatUnusedAnno) :
            "GLB of @I18nFormat(UNUSED) and @I18nUnknownFormat is not @I18nFormat(UNUSED)!";
        // Computing the GLB of @I18nFormat with a value and @I18nFormat(null) should never occur in practice. Skipping this case as it causes an expected crash.
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatUnusedAnno, I18nFormatUnusedAnno), I18nFormatUnusedAnno) :
            "GLB of @I18nFormat(UNUSED) and @I18nFormat(UNUSED) is not @I18nFormat(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatUnusedAnno, I18NINVALIDFORMAT), I18NFORMATBOTTOM) :
            "GLB of @I18nFormat(UNUSED) and @I18nInvalidFormat(null) is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatUnusedAnno, I18nInvalidFormatWithMessage), I18NFORMATBOTTOM) :
            "GLB of @I18nFormat(UNUSED) and @I18nInvalidFormat(\"Message\") is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatUnusedAnno, I18NFORMATFOR), I18NFORMATBOTTOM) :
            "GLB of @I18nFormat(UNUSED) and @I18nFormatFor(null) is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatUnusedAnno, I18nFormatForWithValue1), I18NFORMATBOTTOM) :
            "GLB of @I18nFormat(UNUSED) and @I18nFormatFor(\"#1\") is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatUnusedAnno, I18NFORMATBOTTOM), I18NFORMATBOTTOM) :
            "GLB of @I18nFormat(UNUSED) and @I18nFormatBottom is not @I18nFormatBottom!";

        // GLB of @I18nInvalidFormat(null) and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NINVALIDFORMAT, I18NUNKNOWNFORMAT), I18NINVALIDFORMAT) :
            "GLB of @I18nInvalidFormat(null) and @I18nUnknownFormat is not @I18nInvalidFormat(null)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NINVALIDFORMAT, I18NFORMAT), I18NFORMATBOTTOM) :
            "GLB of @I18nInvalidFormat(null) and @I18nFormat(null) is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NINVALIDFORMAT, I18nFormatUnusedAnno), I18NFORMATBOTTOM) :
            "GLB of @I18nInvalidFormat(null) and @I18nFormat(UNUSED) is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NINVALIDFORMAT, I18NINVALIDFORMAT), I18NINVALIDFORMAT) :
            "GLB of @I18nInvalidFormat(null) and @I18nInvalidFormat(null) is not @I18nInvalidFormat(null)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NINVALIDFORMAT, I18nInvalidFormatWithMessage), I18NINVALIDFORMAT) :
            "GLB of @I18nInvalidFormat(null) and @I18nInvalidFormat(\"Message\") is not @I18nInvalidFormat(null)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NINVALIDFORMAT, I18NFORMATFOR), I18NFORMATBOTTOM) :
            "GLB of @I18nInvalidFormat(null) and @I18nFormatFor(null) is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NINVALIDFORMAT, I18nFormatForWithValue1), I18NFORMATBOTTOM) :
            "GLB of @I18nInvalidFormat(null) and @I18nFormatFor(\"#1\") is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NINVALIDFORMAT, I18NFORMATBOTTOM), I18NFORMATBOTTOM) :
            "GLB of @I18nInvalidFormat(null) and @I18nFormatBottom is not @I18nFormatBottom!";

        // GLB of @I18nInvalidFormat("Message") and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nInvalidFormatWithMessage, I18NUNKNOWNFORMAT), I18nInvalidFormatWithMessage) :
            "GLB of @I18nInvalidFormat(\"Message\") and @I18nUnknownFormat is not @I18nInvalidFormat(\"Message\")!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nInvalidFormatWithMessage, I18NFORMAT), I18NFORMATBOTTOM) :
            "GLB of @I18nInvalidFormat(\"Message\") and @I18nFormat(null) is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nInvalidFormatWithMessage, I18nFormatUnusedAnno), I18NFORMATBOTTOM) :
            "GLB of @I18nInvalidFormat(\"Message\") and @I18nFormat(UNUSED) is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nInvalidFormatWithMessage, I18NINVALIDFORMAT), I18NINVALIDFORMAT) :
            "GLB of @I18nInvalidFormat(\"Message\") and @I18nInvalidFormat(null) is not @I18nInvalidFormat(null)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nInvalidFormatWithMessage, I18nInvalidFormatWithMessage), I18nInvalidFormatWithMessage) :
            "GLB of @I18nInvalidFormat(\"Message\") and @I18nInvalidFormat(\"Message\") is not @I18nInvalidFormat(\"Message\")!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nInvalidFormatWithMessage, I18NFORMATFOR), I18NFORMATBOTTOM) :
            "GLB of @I18nInvalidFormat(\"Message\") and @I18nFormatFor(null) is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nInvalidFormatWithMessage, I18nFormatForWithValue1), I18NFORMATBOTTOM) :
            "GLB of @I18nInvalidFormat(\"Message\") and @I18nFormatFor(\"#1\") is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nInvalidFormatWithMessage, I18NFORMATBOTTOM), I18NFORMATBOTTOM) :
            "GLB of @I18nInvalidFormat(\"Message\") and @I18nFormatBottom is not @I18nFormatBottom!";

        // GLB of @I18nFormatFor(null) and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NFORMATFOR, I18NUNKNOWNFORMAT), I18NFORMATFOR) :
            "GLB of @I18nFormatFor(null) and @I18nUnknownFormat is not @I18nFormatFor(null)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NFORMATFOR, I18NFORMAT), I18NFORMATBOTTOM) :
            "GLB of @I18nFormatFor(null) and @I18nFormat(null) is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NFORMATFOR, I18nFormatUnusedAnno), I18NFORMATBOTTOM) :
            "GLB of @I18nFormatFor(null) and @I18nFormat(UNUSED) is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NFORMATFOR, I18NINVALIDFORMAT), I18NFORMATBOTTOM) :
            "GLB of @I18nFormatFor(null) and @I18nInvalidFormat(null) is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NFORMATFOR, I18nInvalidFormatWithMessage), I18NFORMATBOTTOM) :
            "GLB of @I18nFormatFor(null) and @I18nInvalidFormat(\"Message\") is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NFORMATFOR, I18NFORMATFOR), I18NFORMATFOR) :
            "GLB of @I18nFormatFor(null) and @I18nFormatFor(null) is not @I18nFormatFor(null)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NFORMATFOR, I18nFormatForWithValue1), I18NFORMATBOTTOM) :
            "GLB of @I18nFormatFor(null) and @I18nFormatFor(\"#1\") is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NFORMATFOR, I18NFORMATBOTTOM), I18NFORMATBOTTOM) :
            "GLB of @I18nFormatFor(null) and @I18nFormatBottom is not @I18nFormatBottom!";

        // GLB of @I18nFormatFor("#1") and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatForWithValue1, I18NUNKNOWNFORMAT), I18nFormatForWithValue1) :
            "GLB of @I18nFormatFor(\"#1\") and @I18nUnknownFormat is not @I18nFormatFor(\"#1\")!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatForWithValue1, I18NFORMAT), I18NFORMATBOTTOM) :
            "GLB of @I18nFormatFor(\"#1\") and @I18nFormat(null) is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatForWithValue1, I18nFormatUnusedAnno), I18NFORMATBOTTOM) :
            "GLB of @I18nFormatFor(\"#1\") and @I18nFormat(UNUSED) is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatForWithValue1, I18NINVALIDFORMAT), I18NFORMATBOTTOM) :
            "GLB of @I18nFormatFor(\"#1\") and @I18nInvalidFormat(null) is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatForWithValue1, I18nInvalidFormatWithMessage), I18NFORMATBOTTOM) :
            "GLB of @I18nFormatFor(\"#1\") and @I18nInvalidFormat(\"Message\") is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatForWithValue1, I18NFORMATFOR), I18NFORMATBOTTOM) :
            "GLB of @I18nFormatFor(\"#1\") and @I18nFormatFor(null) is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatForWithValue1, I18nFormatForWithValue1), I18nFormatForWithValue1) :
            "GLB of @I18nFormatFor(\"#1\") and @I18nFormatFor(\"#1\") is not @I18nFormatFor(\"#1\")!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18nFormatForWithValue1, I18NFORMATBOTTOM), I18NFORMATBOTTOM) :
            "GLB of @I18nFormatFor(\"#1\") and @I18nFormatBottom is not @I18nFormatBottom!";

        // GLB of @I18nFormatBottom and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NFORMATBOTTOM, I18NUNKNOWNFORMAT), I18NFORMATBOTTOM) :
            "GLB of @I18nFormatBottom and @I18nUnknownFormat is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NFORMATBOTTOM, I18NFORMAT), I18NFORMATBOTTOM) :
            "GLB of @I18nFormatBottom and @I18nFormat(null) is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NFORMATBOTTOM, I18nFormatUnusedAnno), I18NFORMATBOTTOM) :
            "GLB of @I18nFormatBottom and @I18nFormat(UNUSED) is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NFORMATBOTTOM, I18NINVALIDFORMAT), I18NFORMATBOTTOM) :
            "GLB of @I18nFormatBottom and @I18nInvalidFormat(null) is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NFORMATBOTTOM, I18nInvalidFormatWithMessage), I18NFORMATBOTTOM) :
            "GLB of @I18nFormatBottom and @I18nInvalidFormat(\"Message\") is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NFORMATBOTTOM, I18NFORMATFOR), I18NFORMATBOTTOM) :
            "GLB of @I18nFormatBottom and @I18nFormatFor(null) is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NFORMATBOTTOM, I18nFormatForWithValue1), I18NFORMATBOTTOM) :
            "GLB of @I18nFormatBottom and @I18nFormatFor(\"#1\") is not @I18nFormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(I18NFORMATBOTTOM, I18NFORMATBOTTOM), I18NFORMATBOTTOM) :
            "GLB of @I18nFormatBottom and @I18nFormatBottom is not @I18nFormatBottom!";
        }

}
