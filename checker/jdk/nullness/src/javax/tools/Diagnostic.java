/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package javax.tools;

import java.util.Locale;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Interface for diagnostics from tools.  A diagnostic usually reports
 * a problem at a specific position in a source file.  However, not
 * all diagnostics are associated with a position or a file.
 *
 * <p>A position is a zero-based character offset from the beginning of
 * a file.  Negative values (except {@link #NOPOS}) are not valid
 * positions.
 *
 * <p>Line and column numbers begin at 1.  Negative values (except
 * {@link #NOPOS}) and 0 are not valid line or column numbers.
 *
 * @param <S> the type of source object used by this diagnostic
 *
 * @author Peter von der Ah&eacute;
 * @author Jonathan Gibbons
 * @since 1.6
 */
public interface Diagnostic<S> {

    /**
     * Kinds of diagnostics, for example, error or warning.
     *
     * The kind of a diagnostic can be used to determine how the
     * diagnostic should be presented to the user. For example,
     * errors might be colored red or prefixed with the word "Error",
     * while warnings might be colored yellow or prefixed with the
     * word "Warning". There is no requirement that the Kind
     * should imply any inherent semantic meaning to the message
     * of the diagnostic: for example, a tool might provide an
     * option to report all warnings as errors.
     */
    enum Kind {
        /**
         * Problem which prevents the tool's normal completion.
         */
        ERROR,
        /**
         * Problem which does not usually prevent the tool from
         * completing normally.
         */
        WARNING,
        /**
         * Problem similar to a warning, but is mandated by the tool's
         * specification.  For example, the Java&trade; Language
         * Specification mandates warnings on certain
         * unchecked operations and the use of deprecated methods.
         */
        MANDATORY_WARNING,
        /**
         * Informative message from the tool.
         */
        NOTE,
        /**
         * Diagnostic which does not fit within the other kinds.
         */
        OTHER,
    }

    /**
     * Used to signal that no position is available.
     */
    public final static long NOPOS = -1;

    /**
     * Gets the kind of this diagnostic, for example, error or
     * warning.
     * @return the kind of this diagnostic
     */
    Kind getKind();

    /**
     * Gets the source object associated with this diagnostic.
     *
     * @return the source object associated with this diagnostic.
     * {@code null} if no source object is associated with the
     * diagnostic.
     */
    @Nullable S getSource();

    /**
     * Gets a character offset from the beginning of the source object
     * associated with this diagnostic that indicates the location of
     * the problem.  In addition, the following must be true:
     *
     * <p>{@code getStartPostion() <= getPosition()}
     * <p>{@code getPosition() <= getEndPosition()}
     *
     * @return character offset from beginning of source; {@link
     * #NOPOS} if {@link #getSource()} would return {@code null} or if
     * no location is suitable
     */
    long getPosition();

    /**
     * Gets the character offset from the beginning of the file
     * associated with this diagnostic that indicates the start of the
     * problem.
     *
     * @return offset from beginning of file; {@link #NOPOS} if and
     * only if {@link #getPosition()} returns {@link #NOPOS}
     */
    long getStartPosition();

    /**
     * Gets the character offset from the beginning of the file
     * associated with this diagnostic that indicates the end of the
     * problem.
     *
     * @return offset from beginning of file; {@link #NOPOS} if and
     * only if {@link #getPosition()} returns {@link #NOPOS}
     */
    long getEndPosition();

    /**
     * Gets the line number of the character offset returned by
     * {@linkplain #getPosition()}.
     *
     * @return a line number or {@link #NOPOS} if and only if {@link
     * #getPosition()} returns {@link #NOPOS}
     */
    long getLineNumber();

    /**
     * Gets the column number of the character offset returned by
     * {@linkplain #getPosition()}.
     *
     * @return a column number or {@link #NOPOS} if and only if {@link
     * #getPosition()} returns {@link #NOPOS}
     */
    long getColumnNumber();

    /**
     * Gets a diagnostic code indicating the type of diagnostic.  The
     * code is implementation-dependent and might be {@code null}.
     *
     * @return a diagnostic code
     */
    @Nullable String getCode();

    /**
     * Gets a localized message for the given locale.  The actual
     * message is implementation-dependent.  If the locale is {@code
     * null} use the default locale.
     *
     * @param locale a locale; might be {@code null}
     * @return a localized message
     */
    String getMessage(@Nullable Locale locale);
}
