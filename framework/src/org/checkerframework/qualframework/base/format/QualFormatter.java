package org.checkerframework.qualframework.base.format;

/**
 * Qual to String formatter.
 */
public interface QualFormatter<Q> {

    /**
     * Format a qualifier into a String. Prints invisible qualifiers.
     *
     * @param q the qualifier
     * @return the String representation of q
     */
    String format(Q q);

    /**
     * Format a qualifier into a String.
     *
     * @param q  the qualifier
     * @param printInvisibles if true, invisible qualifiers will be printed
     * @return the String representation of q, or null if q was invisible
     *      and print invisible was false
     */
    String format(Q q, boolean printInvisibles);
}
