package org.checkerframework.qualframework.poly.format;

import org.checkerframework.qualframework.poly.QualParams;
import org.checkerframework.qualframework.poly.format.SurfaceSyntaxQualParamsFormatter.AnnotationParts;

/**
 * SurfaceSyntaxFormatterConfiguration provides values and methods required by
 * the SurfaceSyntaxQualParamsFormatter to format a QualParams into an annotation.
 */
public abstract class SurfaceSyntaxFormatterConfiguration<Q> {

    /** The top ground qualifier **/
    private Q top;
    /** The bottom ground qualifier **/
    private Q bottom;
    /** The top qualifier in the QualParams hierarchy **/
    private QualParams<Q> qualTop;
    /** The bottom ground qualifier **/
    private QualParams<Q> qualBottom;

    public SurfaceSyntaxFormatterConfiguration(Q top, Q bottom, QualParams<Q> qualTop, QualParams<Q> qualBottom) {
        this.top = top;
        this.bottom = bottom;
        this.qualTop = qualTop;
        this.qualBottom = qualBottom;
    }

    /**
     * Determine if the annotation should be printed
     *
     * @param anno the AnnotationParts representation of the annotatino
     * @param printInvisibleQualifiers if printingInvisibleQualifiers is enabled
     * @return true if the annotation should be printed
     */
    protected abstract boolean shouldPrintAnnotation(AnnotationParts anno, boolean printInvisibleQualifiers);

    /**
     * Return an AnnotationParts object that represents the equivalent
     * annotation of q.
     *
     * @param qual the qualifier
     * @return the AnnotationParts
     */
    protected abstract AnnotationParts getTargetTypeSystemAnnotation(Q qual);

    public Q getBottom() {
        return bottom;
    }

    public Q getTop() {
        return top;
    }

    public QualParams<Q> getQualBottom() {
        return qualBottom;
    }

    public QualParams<Q> getQualTop() {
        return qualTop;
    }
}