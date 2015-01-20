package org.checkerframework.qualframework.base.format;

import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.base.format.QualFormatter;

/**
 * Interface to format a QualifiedTypeMirrors or an individual qualifier.
 */
public interface QualifiedTypeFormatter<Q> {

    /**
     * Format a QualifiedTypeMirror into a String. Prints invisible qualifiers.
     *
     * @param qtm the QualifiedTypeMirror
     * @return the String representation of qtm
     */
    String format(QualifiedTypeMirror<Q> qtm);

    /**
     * Format a QualifiedTypeMirror into a String. Prints invisible qualifiers.
     *
     * @param qtm the QualifiedTypeMirror
     * @param printInvisibles if true, invisible qualifiers will be printed
     * @return the String representation of qtm
     */
    String format(QualifiedTypeMirror<Q> qtm, boolean printInvisibles);

    /**
     * Return the QualFormatter used by this formatter.
     *
     * @return the QualFormatter
     */
    QualFormatter<Q> getQualFormatter();
}
