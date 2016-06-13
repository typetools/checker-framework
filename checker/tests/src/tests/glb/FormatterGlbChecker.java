package tests.glb;

// Test case for issue 691.
// https://github.com/typetools/checker-framework/issues/691

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

import org.checkerframework.checker.formatter.FormatterChecker;
import org.checkerframework.checker.formatter.FormatterTreeUtil;
import org.checkerframework.checker.formatter.qual.ConversionCategory;
import org.checkerframework.checker.formatter.qual.Format;
import org.checkerframework.checker.formatter.qual.FormatBottom;
import org.checkerframework.checker.formatter.qual.InvalidFormat;
import org.checkerframework.checker.formatter.qual.UnknownFormat;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * This class tests the implementation of GLB computation in the
 * Formatter Checker, but it does not test for the crash described
 * in issue 691. That is done by tests/all-systems/Issue691.java.
 */
public class FormatterGlbChecker extends FormatterChecker {

    @Override
    public void initChecker() {
        super.initChecker();
        FormatterTreeUtil treeUtil = new FormatterTreeUtil(this);

      Elements elements = getElementUtils();
      AnnotationMirror UNKNOWNFORMAT = AnnotationUtils.fromClass(elements, UnknownFormat.class);
        AnnotationMirror FORMAT = AnnotationUtils.fromClass(elements, Format.class);
        AnnotationMirror INVALIDFORMAT = AnnotationUtils.fromClass(elements, InvalidFormat.class);
        AnnotationMirror FORMATBOTTOM = AnnotationUtils.fromClass(elements, FormatBottom.class);

        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, InvalidFormat.class.getCanonicalName());
        builder.setValue("value", "Message");
        AnnotationMirror InvalidFormatWithMessage = builder.build();

        ConversionCategory[] cc = new ConversionCategory[1];

        cc[0] = ConversionCategory.UNUSED;
        AnnotationMirror FormatUnusedAnno = treeUtil.categoriesToFormatAnnotation(cc);
        cc[0] = ConversionCategory.GENERAL;
        AnnotationMirror FormatGeneralAnno = treeUtil.categoriesToFormatAnnotation(cc);
        cc[0] = ConversionCategory.CHAR;
        AnnotationMirror FormatCharAnno = treeUtil.categoriesToFormatAnnotation(cc);
        cc[0] = ConversionCategory.INT;
        AnnotationMirror FormatIntAnno = treeUtil.categoriesToFormatAnnotation(cc);
        cc[0] = ConversionCategory.TIME;
        AnnotationMirror FormatTimeAnno = treeUtil.categoriesToFormatAnnotation(cc);
        cc[0] = ConversionCategory.FLOAT;
        AnnotationMirror FormatFloatAnno = treeUtil.categoriesToFormatAnnotation(cc);
        cc[0] = ConversionCategory.CHAR_AND_INT;
        AnnotationMirror FormatCharAndIntAnno = treeUtil.categoriesToFormatAnnotation(cc);
        cc[0] = ConversionCategory.INT_AND_TIME;
        AnnotationMirror FormatIntAndTimeAnno = treeUtil.categoriesToFormatAnnotation(cc);
        cc[0] = ConversionCategory.NULL;
        AnnotationMirror FormatNullAnno = treeUtil.categoriesToFormatAnnotation(cc);

        QualifierHierarchy qh = ((BaseTypeVisitor<?>)visitor).getTypeFactory().getQualifierHierarchy();

        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatCharAndIntAnno, FormatIntAndTimeAnno), FormatIntAnno) :
            "GLB of @Format(CHAR_AND_INT) and @Format(INT_AND_TIME) is not @Format(INT)!";

        // GLB of UNUSED and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatUnusedAnno, FormatUnusedAnno), FormatUnusedAnno) :
            "GLB of @Format(UNUSED) and @Format(UNUSED) is not @Format(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatUnusedAnno, FormatGeneralAnno), FormatUnusedAnno) :
            "GLB of @Format(UNUSED) and @Format(GENERAL) is not @Format(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatUnusedAnno, FormatCharAnno), FormatUnusedAnno) :
            "GLB of @Format(UNUSED) and @Format(CHAR) is not @Format(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatUnusedAnno, FormatIntAnno), FormatUnusedAnno) :
            "GLB of @Format(UNUSED) and @Format(INT) is not @Format(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatUnusedAnno, FormatTimeAnno), FormatUnusedAnno) :
            "GLB of @Format(UNUSED) and @Format(TIME) is not @Format(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatUnusedAnno, FormatFloatAnno), FormatUnusedAnno) :
            "GLB of @Format(UNUSED) and @Format(FLOAT) is not @Format(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatUnusedAnno, FormatCharAndIntAnno), FormatUnusedAnno) :
            "GLB of @Format(UNUSED) and @Format(CHAR_AND_INT) is not @Format(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatUnusedAnno, FormatIntAndTimeAnno), FormatUnusedAnno) :
            "GLB of @Format(UNUSED) and @Format(INT_AND_TIME) is not @Format(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatUnusedAnno, FormatNullAnno), FormatUnusedAnno) :
            "GLB of @Format(UNUSED) and @Format(NULL) is not @Format(UNUSED)!";

        // GLB of GENERAL and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatGeneralAnno, FormatUnusedAnno), FormatUnusedAnno) :
            "GLB of @Format(GENERAL) and @Format(UNUSED) is not @Format(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatGeneralAnno, FormatGeneralAnno), FormatGeneralAnno) :
            "GLB of @Format(GENERAL) and @Format(GENERAL) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatGeneralAnno, FormatCharAnno), FormatGeneralAnno) :
            "GLB of @Format(GENERAL) and @Format(CHAR) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatGeneralAnno, FormatIntAnno), FormatGeneralAnno) :
            "GLB of @Format(GENERAL) and @Format(INT) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatGeneralAnno, FormatTimeAnno), FormatGeneralAnno) :
            "GLB of @Format(GENERAL) and @Format(TIME) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatGeneralAnno, FormatFloatAnno), FormatGeneralAnno) :
            "GLB of @Format(GENERAL) and @Format(FLOAT) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatGeneralAnno, FormatCharAndIntAnno), FormatGeneralAnno) :
            "GLB of @Format(GENERAL) and @Format(CHAR_AND_INT) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatGeneralAnno, FormatIntAndTimeAnno), FormatGeneralAnno) :
            "GLB of @Format(GENERAL) and @Format(INT_AND_TIME) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatGeneralAnno, FormatNullAnno), FormatGeneralAnno) :
            "GLB of @Format(GENERAL) and @Format(NULL) is not @Format(GENERAL)!";

        // GLB of CHAR and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatCharAnno, FormatUnusedAnno), FormatUnusedAnno) :
            "GLB of @Format(CHAR) and @Format(UNUSED) is not @Format(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatCharAnno, FormatGeneralAnno), FormatGeneralAnno) :
            "GLB of @Format(CHAR) and @Format(GENERAL) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatCharAnno, FormatCharAnno), FormatCharAnno) :
            "GLB of @Format(CHAR) and @Format(CHAR) is not @Format(CHAR)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatCharAnno, FormatIntAnno), FormatGeneralAnno) :
            "GLB of @Format(CHAR) and @Format(INT) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatCharAnno, FormatTimeAnno), FormatGeneralAnno) :
            "GLB of @Format(CHAR) and @Format(TIME) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatCharAnno, FormatFloatAnno), FormatGeneralAnno) :
            "GLB of @Format(CHAR) and @Format(FLOAT) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatCharAnno, FormatCharAndIntAnno), FormatCharAnno) :
            "GLB of @Format(CHAR) and @Format(CHAR_AND_INT) is not @Format(CHAR)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatCharAnno, FormatIntAndTimeAnno), FormatGeneralAnno) :
            "GLB of @Format(CHAR) and @Format(INT_AND_TIME) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatCharAnno, FormatNullAnno), FormatCharAnno) :
            "GLB of @Format(CHAR) and @Format(NULL) is not @Format(CHAR)!";

        // GLB of INT and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatIntAnno, FormatUnusedAnno), FormatUnusedAnno) :
            "GLB of @Format(INT) and @Format(UNUSED) is not @Format(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatIntAnno, FormatGeneralAnno), FormatGeneralAnno) :
            "GLB of @Format(INT) and @Format(GENERAL) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatIntAnno, FormatCharAnno), FormatGeneralAnno) :
            "GLB of @Format(INT) and @Format(CHAR) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatIntAnno, FormatIntAnno), FormatIntAnno) :
            "GLB of @Format(INT) and @Format(INT) is not @Format(INT)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatIntAnno, FormatTimeAnno), FormatGeneralAnno) :
            "GLB of @Format(INT) and @Format(TIME) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatIntAnno, FormatFloatAnno), FormatGeneralAnno) :
            "GLB of @Format(INT) and @Format(FLOAT) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatIntAnno, FormatCharAndIntAnno), FormatIntAnno) :
            "GLB of @Format(INT) and @Format(CHAR_AND_INT) is not @Format(INT)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatIntAnno, FormatIntAndTimeAnno), FormatIntAnno) :
            "GLB of @Format(INT) and @Format(INT_AND_TIME) is not @Format(INT)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatIntAnno, FormatNullAnno), FormatIntAnno) :
            "GLB of @Format(INT) and @Format(NULL) is not @Format(INT)!";

        // GLB of TIME and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatTimeAnno, FormatUnusedAnno), FormatUnusedAnno) :
            "GLB of @Format(TIME) and @Format(UNUSED) is not @Format(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatTimeAnno, FormatGeneralAnno), FormatGeneralAnno) :
            "GLB of @Format(TIME) and @Format(GENERAL) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatTimeAnno, FormatCharAnno), FormatGeneralAnno) :
            "GLB of @Format(TIME) and @Format(CHAR) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatTimeAnno, FormatIntAnno), FormatGeneralAnno) :
            "GLB of @Format(TIME) and @Format(INT) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatTimeAnno, FormatTimeAnno), FormatTimeAnno) :
            "GLB of @Format(TIME) and @Format(TIME) is not @Format(TIME)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatTimeAnno, FormatFloatAnno), FormatGeneralAnno) :
            "GLB of @Format(TIME) and @Format(FLOAT) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatTimeAnno, FormatCharAndIntAnno), FormatGeneralAnno) :
            "GLB of @Format(TIME) and @Format(CHAR_AND_INT) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatTimeAnno, FormatIntAndTimeAnno), FormatTimeAnno) :
            "GLB of @Format(TIME) and @Format(INT_AND_TIME) is not @Format(TIME)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatTimeAnno, FormatNullAnno), FormatTimeAnno) :
            "GLB of @Format(TIME) and @Format(NULL) is not @Format(TIME)!";

        // GLB of FLOAT and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatFloatAnno, FormatUnusedAnno), FormatUnusedAnno) :
            "GLB of @Format(FLOAT) and @Format(UNUSED) is not @Format(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatFloatAnno, FormatGeneralAnno), FormatGeneralAnno) :
            "GLB of @Format(FLOAT) and @Format(GENERAL) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatFloatAnno, FormatCharAnno), FormatGeneralAnno) :
            "GLB of @Format(FLOAT) and @Format(CHAR) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatFloatAnno, FormatIntAnno), FormatGeneralAnno) :
            "GLB of @Format(FLOAT) and @Format(INT) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatFloatAnno, FormatTimeAnno), FormatGeneralAnno) :
            "GLB of @Format(FLOAT) and @Format(TIME) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatFloatAnno, FormatFloatAnno), FormatFloatAnno) :
            "GLB of @Format(FLOAT) and @Format(FLOAT) is not @Format(FLOAT)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatFloatAnno, FormatCharAndIntAnno), FormatGeneralAnno) :
            "GLB of @Format(FLOAT) and @Format(CHAR_AND_INT) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatFloatAnno, FormatIntAndTimeAnno), FormatGeneralAnno) :
            "GLB of @Format(FLOAT) and @Format(INT_AND_TIME) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatFloatAnno, FormatNullAnno), FormatFloatAnno) :
            "GLB of @Format(FLOAT) and @Format(NULL) is not @Format(FLOAT)!";

        // GLB of CHAR_AND_INT and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatCharAndIntAnno, FormatUnusedAnno), FormatUnusedAnno) :
            "GLB of @Format(CHAR_AND_INT) and @Format(UNUSED) is not @Format(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatCharAndIntAnno, FormatGeneralAnno), FormatGeneralAnno) :
            "GLB of @Format(CHAR_AND_INT) and @Format(GENERAL) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatCharAndIntAnno, FormatCharAnno), FormatCharAnno) :
            "GLB of @Format(CHAR_AND_INT) and @Format(CHAR) is not @Format(CHAR)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatCharAndIntAnno, FormatIntAnno), FormatIntAnno) :
            "GLB of @Format(CHAR_AND_INT) and @Format(INT) is not @Format(INT)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatCharAndIntAnno, FormatTimeAnno), FormatGeneralAnno) :
            "GLB of @Format(CHAR_AND_INT) and @Format(TIME) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatCharAndIntAnno, FormatFloatAnno), FormatGeneralAnno) :
            "GLB of @Format(CHAR_AND_INT) and @Format(FLOAT) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatCharAndIntAnno, FormatCharAndIntAnno), FormatCharAndIntAnno) :
            "GLB of @Format(CHAR_AND_INT) and @Format(CHAR_AND_INT) is not @Format(CHAR_AND_INT)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatCharAndIntAnno, FormatIntAndTimeAnno), FormatIntAnno) :
            "GLB of @Format(CHAR_AND_INT) and @Format(INT_AND_TIME) is not @Format(INT)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatCharAndIntAnno, FormatNullAnno), FormatCharAndIntAnno) :
            "GLB of @Format(CHAR_AND_INT) and @Format(NULL) is not @Format(CHAR_AND_INT)!";

        // GLB of INT_AND_TIME and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatIntAndTimeAnno, FormatUnusedAnno), FormatUnusedAnno) :
            "GLB of @Format(INT_AND_TIME) and @Format(UNUSED) is not @Format(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatIntAndTimeAnno, FormatGeneralAnno), FormatGeneralAnno) :
            "GLB of @Format(INT_AND_TIME) and @Format(GENERAL) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatIntAndTimeAnno, FormatCharAnno), FormatGeneralAnno) :
            "GLB of @Format(INT_AND_TIME) and @Format(CHAR) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatIntAndTimeAnno, FormatIntAnno), FormatIntAnno) :
            "GLB of @Format(INT_AND_TIME) and @Format(INT) is not @Format(INT)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatIntAndTimeAnno, FormatTimeAnno), FormatTimeAnno) :
            "GLB of @Format(INT_AND_TIME) and @Format(TIME) is not @Format(TIME)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatIntAndTimeAnno, FormatFloatAnno), FormatGeneralAnno) :
            "GLB of @Format(INT_AND_TIME) and @Format(FLOAT) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatIntAndTimeAnno, FormatCharAndIntAnno), FormatIntAnno) :
            "GLB of @Format(INT_AND_TIME) and @Format(CHAR_AND_INT) is not @Format(INT)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatIntAndTimeAnno, FormatIntAndTimeAnno), FormatIntAndTimeAnno) :
            "GLB of @Format(INT_AND_TIME) and @Format(INT_AND_TIME) is not @Format(INT_AND_TIME)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatIntAndTimeAnno, FormatNullAnno), FormatIntAndTimeAnno) :
            "GLB of @Format(INT_AND_TIME) and @Format(NULL) is not @Format(INT_AND_TIME)!";

        // GLB of NULL and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatNullAnno, FormatUnusedAnno), FormatUnusedAnno) :
            "GLB of @Format(NULL) and @Format(UNUSED) is not @Format(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatNullAnno, FormatGeneralAnno), FormatGeneralAnno) :
            "GLB of @Format(NULL) and @Format(GENERAL) is not @Format(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatNullAnno, FormatCharAnno), FormatCharAnno) :
            "GLB of @Format(NULL) and @Format(CHAR) is not @Format(CHAR)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatNullAnno, FormatIntAnno), FormatIntAnno) :
            "GLB of @Format(NULL) and @Format(INT) is not @Format(INT)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatNullAnno, FormatTimeAnno), FormatTimeAnno) :
            "GLB of @Format(NULL) and @Format(TIME) is not @Format(TIME)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatNullAnno, FormatFloatAnno), FormatFloatAnno) :
            "GLB of @Format(NULL) and @Format(FLOAT) is not @Format(FLOAT)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatNullAnno, FormatCharAndIntAnno), FormatCharAndIntAnno) :
            "GLB of @Format(NULL) and @Format(CHAR_AND_INT) is not @Format(CHAR_AND_INT)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatNullAnno, FormatIntAndTimeAnno), FormatIntAndTimeAnno) :
            "GLB of @Format(NULL) and @Format(INT_AND_TIME) is not @Format(INT_AND_TIME)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatNullAnno, FormatNullAnno), FormatNullAnno) :
            "GLB of @Format(NULL) and @Format(NULL) is not @Format(NULL)!";

        // Now test with two ConversionCategory at a time:

        ConversionCategory[] cc2 = new ConversionCategory[2];

        cc2[0] = ConversionCategory.CHAR_AND_INT;
        cc2[1] = ConversionCategory.FLOAT;
        AnnotationMirror FormatTwoConvCat1 = treeUtil.categoriesToFormatAnnotation(cc2);
        cc2[0] = ConversionCategory.INT;
        cc2[1] = ConversionCategory.CHAR;
        AnnotationMirror FormatTwoConvCat2 = treeUtil.categoriesToFormatAnnotation(cc2);
        cc2[0] = ConversionCategory.INT;
        cc2[1] = ConversionCategory.GENERAL;
        AnnotationMirror FormatTwoConvCat3 = treeUtil.categoriesToFormatAnnotation(cc2);

        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatTwoConvCat1, FormatTwoConvCat2), FormatTwoConvCat3) :
            "GLB of @Format([CHAR_AND_INT,FLOAT]) and @Format([INT,CHAR]) is not @Format([INT,GENERAL])!";

        // Test that the GLB of two ConversionCategory arrays of different sizes is an array of the smallest size of the two:

        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatGeneralAnno, FormatTwoConvCat1), FormatGeneralAnno) :
            "GLB of @I18nFormat(GENERAL) and @I18nFormat([CHAR_AND_INT,FLOAT]) is not @I18nFormat(GENERAL)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatTwoConvCat2, FormatNullAnno), FormatIntAnno) :
            "GLB of @I18nFormat([INT,CHAR]) and @I18nFormat(NULL) is not @I18nFormat(INT)!";

        // GLB of @UnknownFormat and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(UNKNOWNFORMAT, UNKNOWNFORMAT), UNKNOWNFORMAT) :
            "GLB of @UnknownFormat and @UnknownFormat is not @UnknownFormat!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(UNKNOWNFORMAT, FORMAT), FORMAT) :
            "GLB of @UnknownFormat and @Format(null) is not @Format(null)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(UNKNOWNFORMAT, FormatUnusedAnno), FormatUnusedAnno) :
            "GLB of @UnknownFormat and @Format(UNUSED) is not @Format(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(UNKNOWNFORMAT, INVALIDFORMAT), INVALIDFORMAT) :
            "GLB of @UnknownFormat and @InvalidFormat(null) is not @InvalidFormat(null)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(UNKNOWNFORMAT, InvalidFormatWithMessage), InvalidFormatWithMessage) :
            "GLB of @UnknownFormat and @InvalidFormat(\"Message\") is not @InvalidFormat(\"Message\")!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(UNKNOWNFORMAT, FORMATBOTTOM), FORMATBOTTOM) :
            "GLB of @UnknownFormat and @FormatBottom is not @FormatBottom!";

        // GLB of @Format(null) and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(FORMAT, UNKNOWNFORMAT), FORMAT) :
            "GLB of @Format(null) and @UnknownFormat is not @Format(null)!";
        // Computing the GLB of @Format(null) and @Format(null) should never occur in practice. Skipping this case as it causes an expected crash.
        // Computing the GLB of @Format(null) and @Format with a value should never occur in practice. Skipping this case as it causes an expected crash.
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FORMAT, INVALIDFORMAT), FORMATBOTTOM) :
            "GLB of @Format(null) and @InvalidFormat(null) is not @FormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FORMAT, InvalidFormatWithMessage), FORMATBOTTOM) :
            "GLB of @Format(null) and @InvalidFormat(\"Message\") is not @FormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FORMAT, FORMATBOTTOM), FORMATBOTTOM) :
            "GLB of @Format(null) and @FormatBottom is not @FormatBottom!";

        // GLB of @Format(UNUSED) and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatUnusedAnno, UNKNOWNFORMAT), FormatUnusedAnno) :
            "GLB of @Format(UNUSED) and @UnknownFormat is not @Format(UNUSED)!";
        // Computing the GLB of @Format with a value and @Format(null) should never occur in practice. Skipping this case as it causes an expected crash.
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatUnusedAnno, FormatUnusedAnno), FormatUnusedAnno) :
            "GLB of @Format(UNUSED) and @Format(UNUSED) is not @Format(UNUSED)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatUnusedAnno, INVALIDFORMAT), FORMATBOTTOM) :
            "GLB of @Format(UNUSED) and @InvalidFormat(null) is not @FormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatUnusedAnno, InvalidFormatWithMessage), FORMATBOTTOM) :
            "GLB of @Format(UNUSED) and @InvalidFormat(\"Message\") is not @FormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FormatUnusedAnno, FORMATBOTTOM), FORMATBOTTOM) :
            "GLB of @Format(UNUSED) and @FormatBottom is not @FormatBottom!";

        // GLB of @InvalidFormat(null) and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(INVALIDFORMAT, UNKNOWNFORMAT), INVALIDFORMAT) :
            "GLB of @InvalidFormat(null) and @UnknownFormat is not @InvalidFormat(null)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(INVALIDFORMAT, FORMAT), FORMATBOTTOM) :
            "GLB of @InvalidFormat(null) and @Format(null) is not @FormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(INVALIDFORMAT, FormatUnusedAnno), FORMATBOTTOM) :
            "GLB of @InvalidFormat(null) and @Format(UNUSED) is not @FormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(INVALIDFORMAT, INVALIDFORMAT), INVALIDFORMAT) :
            "GLB of @InvalidFormat(null) and @InvalidFormat(null) is not @InvalidFormat(null)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(INVALIDFORMAT, InvalidFormatWithMessage), INVALIDFORMAT) :
            "GLB of @InvalidFormat(null) and @InvalidFormat(\"Message\") is not @InvalidFormat(null)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(INVALIDFORMAT, FORMATBOTTOM), FORMATBOTTOM) :
            "GLB of @InvalidFormat(null) and @FormatBottom is not @FormatBottom!";

        // GLB of @InvalidFormat("Message") and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(InvalidFormatWithMessage, UNKNOWNFORMAT), InvalidFormatWithMessage) :
            "GLB of @InvalidFormat(\"Message\") and @UnknownFormat is not @InvalidFormat(\"Message\")!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(InvalidFormatWithMessage, FORMAT), FORMATBOTTOM) :
            "GLB of @InvalidFormat(\"Message\") and @Format(null) is not @FormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(InvalidFormatWithMessage, FormatUnusedAnno), FORMATBOTTOM) :
            "GLB of @InvalidFormat(\"Message\") and @Format(UNUSED) is not @FormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(InvalidFormatWithMessage, INVALIDFORMAT), INVALIDFORMAT) :
            "GLB of @InvalidFormat(\"Message\") and @InvalidFormat(null) is not @InvalidFormat(null)!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(InvalidFormatWithMessage, InvalidFormatWithMessage), InvalidFormatWithMessage) :
            "GLB of @InvalidFormat(\"Message\") and @InvalidFormat(\"Message\") is not @InvalidFormat(\"Message\")!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(InvalidFormatWithMessage, FORMATBOTTOM), FORMATBOTTOM) :
            "GLB of @InvalidFormat(\"Message\") and @FormatBottom is not @FormatBottom!";

        // GLB of @FormatBottom and others

        assert AnnotationUtils.areSame(qh.greatestLowerBound(FORMATBOTTOM, UNKNOWNFORMAT), FORMATBOTTOM) :
            "GLB of @FormatBottom and @UnknownFormat is not @FormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FORMATBOTTOM, FORMAT), FORMATBOTTOM) :
            "GLB of @FormatBottom and @Format(null) is not @FormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FORMATBOTTOM, FormatUnusedAnno), FORMATBOTTOM) :
            "GLB of @FormatBottom and @Format(UNUSED) is not @FormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FORMATBOTTOM, INVALIDFORMAT), FORMATBOTTOM) :
            "GLB of @FormatBottom and @InvalidFormat(null) is not @FormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FORMATBOTTOM, InvalidFormatWithMessage), FORMATBOTTOM) :
            "GLB of @FormatBottom and @InvalidFormat(\"Message\") is not @FormatBottom!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(FORMATBOTTOM, FORMATBOTTOM), FORMATBOTTOM) :
            "GLB of @FormatBottom and @FormatBottom is not @FormatBottom!";
        }

}
