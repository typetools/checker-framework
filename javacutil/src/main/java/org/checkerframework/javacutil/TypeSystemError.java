package org.checkerframework.javacutil;

import org.checkerframework.checker.formatter.qual.FormatMethod;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Exception type indicating a mistake by a type system built using the Checker Framework. For
 * example, misusing a meta-annotation on a qualifier.
 *
 * <p>To indicate a bug in the framework, use use {@link BugInCF}. To indicate that an end user made
 * a mistake, use {@link UserError}.
 */
@SuppressWarnings("serial")
public class TypeSystemError extends RuntimeException {

    /**
     * Constructs a new TypeSystemError with the specified detail message.
     *
     * @param message the detail message
     */
    public TypeSystemError(String message) {
        super(message);
        if (message == null) {
            throw new Error("Must have a detail message.");
        }
    }

    /**
     * Constructs a new TypeSystemError with a detail message composed from the given arguments.
     *
     * @param fmt the format string
     * @param args the arguments for the format string
     */
    @FormatMethod
    public TypeSystemError(String fmt, @Nullable Object... args) {
        this(String.format(fmt, args));
    }
}
