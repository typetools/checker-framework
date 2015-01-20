package org.checkerframework.qualframework.poly.format;

import org.checkerframework.qualframework.base.format.QualFormatter;
import org.checkerframework.qualframework.poly.PolyQual;
import org.checkerframework.qualframework.poly.QualParams;

/**
 * A QualFormatter with extra methods to format QualParams.
 */
public interface QualParamsFormatter<Q> extends QualFormatter<QualParams<Q>> {

    /**
     * Format a PolyQual into a string.
     *
     * @param polyQual the PolyQual
     * @param printInvisible if true, invisible qualifiers will be printed
     * @return the String representation of q, or null if q was invisible
     *      and print invisible was false
     */
    String format(PolyQual<Q> polyQual, boolean printInvisible);

    /**
     * Format a QualParams into a string, but optionally skip the primary
     * qualifier.
     *
     * @param polyQual the PolyQual
     * @param printPrimary if true, include the primary qualifier in the output
     * @param printInvisible if true, invisible qualifiers will be printed
     * @return the String representation of q, or null if q was invisible
     *      and print invisible was false
     */
    String format(QualParams<Q> polyQual, boolean printPrimary, boolean printInvisible);

}
