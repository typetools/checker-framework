package org.checkerframework.qualframework.poly.format;

import org.checkerframework.qualframework.poly.PolyQual;
import org.checkerframework.qualframework.poly.QualParams;

/**
 * Create a String representation of a {@link QualParams} or {@link PolyQual}.
 */
public interface QualParamsFormatter<Q> {

    String format(QualParams<Q> params);

    String format(QualParams<Q> params, boolean includePrimaryQualifier);

    String format(PolyQual<Q> polyQual);

}
