package org.checkerframework.checker.index;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A class for functionality common to multiple type-checkers that are used by the Index Checker.
 */
public abstract class BaseAnnotatedTypeFactoryForIndexChecker extends BaseAnnotatedTypeFactory {

    /** The from() element/field of a @HasSubsequence annotation. */
    protected final ExecutableElement hasSubsequenceFromElement;
    /** The to() element/field of a @HasSubsequence annotation. */
    protected final ExecutableElement hasSubsequenceToElement;
    /** The subsequence() element/field of a @HasSubsequence annotation. */
    protected final ExecutableElement hasSubsequenceSubsequenceElement;

    /**
     * Creates a new BaseAnnotatedTypeFactoryForIndexChecker.
     *
     * @param checker the checker
     */
    public BaseAnnotatedTypeFactoryForIndexChecker(BaseTypeChecker checker) {
        super(checker);
        hasSubsequenceFromElement =
                TreeUtils.getMethod(
                        "org.checkerframework.checker.index.qual.HasSubsequence",
                        "from",
                        0,
                        processingEnv);
        hasSubsequenceToElement =
                TreeUtils.getMethod(
                        "org.checkerframework.checker.index.qual.HasSubsequence",
                        "to",
                        0,
                        processingEnv);
        hasSubsequenceSubsequenceElement =
                TreeUtils.getMethod(
                        "org.checkerframework.checker.index.qual.HasSubsequence",
                        "subsequence",
                        0,
                        processingEnv);
    }

    /**
     * Gets the from() element/field out of a HasSubsequence annotation.
     *
     * @param anno a HasSubsequence annotation
     * @return its from() element/field
     */
    public String hasSubsequenceFromValue(AnnotationMirror anno) {
        return (String) anno.getElementValues().get(hasSubsequenceFromElement).getValue();
    }

    /**
     * Gets the to() element/field out of a HasSubsequence annotation.
     *
     * @param anno a HasSubsequence annotation
     * @return its to() element/field
     */
    public String hasSubsequenceToValue(AnnotationMirror anno) {
        return (String) anno.getElementValues().get(hasSubsequenceToElement).getValue();
    }

    /**
     * Gets the subsequence() element/field out of a HasSubsequence annotation.
     *
     * @param anno a HasSubsequence annotation
     * @return its subsequence() element/field
     */
    public String hasSubsequenceSubsequenceValue(AnnotationMirror anno) {
        return (String) anno.getElementValues().get(hasSubsequenceSubsequenceElement).getValue();
    }
}
