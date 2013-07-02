package checkers.formatter;

import javacutils.AnnotationUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

import checkers.formatter.quals.ConversionCategory;
import checkers.formatter.quals.Format;
import checkers.formatter.quals.FormatBottom;
import checkers.formatter.quals.InvalidFormat;
import checkers.util.GraphQualifierHierarchy;

public class FormatterQualifierHierarchy extends GraphQualifierHierarchy {
    private final FormatterChecker checker;
    private final AnnotationMirror FORMAT;
    private final AnnotationMirror INVALIDFORMAT;

    public FormatterQualifierHierarchy(FormatterChecker formatterChecker, ProcessingEnvironment processingEnv, MultiGraphFactory f) {
        super(f, AnnotationUtils.fromClass(processingEnv.getElementUtils(), FormatBottom.class));
        checker = formatterChecker;
        Elements elements = processingEnv.getElementUtils();
        FORMAT = AnnotationUtils.fromClass(elements, Format.class);
        INVALIDFORMAT = AnnotationUtils.fromClass(elements, InvalidFormat.class);
    }

    @Override
    public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
        if (AnnotationUtils.areSameIgnoringValues(rhs, FORMAT) &&
            AnnotationUtils.areSameIgnoringValues(lhs, FORMAT))
        {
            ConversionCategory[] rhsArgTypes =
                    checker.treeUtil.formatAnnotationToCategories(rhs);
            ConversionCategory[] lhsArgTypes =
                    checker.treeUtil.formatAnnotationToCategories(lhs);

            if (rhsArgTypes.length != lhsArgTypes.length) {
                return false;
            }

            for (int i = 0; i < rhsArgTypes.length; ++i) {
                if (!ConversionCategory.isSubsetOf(lhsArgTypes[i], rhsArgTypes[i])) {
                    return false;
                }
            }
            return true;
        }
        if (AnnotationUtils.areSameIgnoringValues(lhs, FORMAT)) {
            lhs = FORMAT;
        }
        if (AnnotationUtils.areSameIgnoringValues(rhs, FORMAT)) {
            rhs = FORMAT;
        }
        if (AnnotationUtils.areSameIgnoringValues(lhs, INVALIDFORMAT)) {
            lhs = INVALIDFORMAT;
        }
        if (AnnotationUtils.areSameIgnoringValues(rhs, INVALIDFORMAT)) {
            rhs = INVALIDFORMAT;
        }

        return super.isSubtype(rhs, lhs);
    }
}
