package org.checkerframework.checker.index;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.index.qual.HasSubsequence;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A class for functionality common to multiple type-checkers that are used by the Index Checker.
 */
public abstract class BaseAnnotatedTypeFactoryForIndexChecker extends BaseAnnotatedTypeFactory {

    /** The from() element/field of a @HasSubsequence annotation. */
    protected final ExecutableElement hasSubsequenceFromElement =
            TreeUtils.getMethod(HasSubsequence.class, "from", 0, processingEnv);
    /** The to() element/field of a @HasSubsequence annotation. */
    protected final ExecutableElement hasSubsequenceToElement =
            TreeUtils.getMethod(HasSubsequence.class, "to", 0, processingEnv);
    /** The subsequence() element/field of a @HasSubsequence annotation. */
    protected final ExecutableElement hasSubsequenceSubsequenceElement =
            TreeUtils.getMethod(HasSubsequence.class, "subsequence", 0, processingEnv);

    /**
     * Creates a new BaseAnnotatedTypeFactoryForIndexChecker.
     *
     * @param checker the checker
     */
    public BaseAnnotatedTypeFactoryForIndexChecker(BaseTypeChecker checker) {
        super(checker);
    }

    /**
     * Gets the from() element/field out of a HasSubsequence annotation.
     *
     * @param anno a HasSubsequence annotation
     * @return its from() element/field
     */
    public String hasSubsequenceFromValue(AnnotationMirror anno) {
        return AnnotationUtils.getElementValue(anno, hasSubsequenceFromElement, String.class);
    }

    /**
     * Gets the to() element/field out of a HasSubsequence annotation.
     *
     * @param anno a HasSubsequence annotation
     * @return its to() element/field
     */
    public String hasSubsequenceToValue(AnnotationMirror anno) {
        return AnnotationUtils.getElementValue(anno, hasSubsequenceToElement, String.class);
    }

    /**
     * Gets the subsequence() element/field out of a HasSubsequence annotation.
     *
     * @param anno a HasSubsequence annotation
     * @return its subsequence() element/field
     */
    public String hasSubsequenceSubsequenceValue(AnnotationMirror anno) {
        return AnnotationUtils.getElementValue(
                anno, hasSubsequenceSubsequenceElement, String.class);
    }
}
