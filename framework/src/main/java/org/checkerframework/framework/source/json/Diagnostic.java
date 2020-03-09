package org.checkerframework.framework.source.json;

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Represents a diagnostic, such as a compiler error or warning. */
public class Diagnostic {
    /** The range at which the message applies. */
    public final Range range;
    /**
     * The diagnostic's severity. Can be omitted. If omitted it is up to the client to interpret
     * diagnostics as error, warning, info or hint.
     */
    public final @Nullable Integer severity;
    /** The diagnostic's code, which might appear in the user interface. */
    public final @Nullable String code;
    /**
     * A human-readable string describing the source of this diagnostic, e.g. 'typescript' or 'super
     * lint'.
     */
    public final @Nullable String source;
    /** The diagnostic's message. */
    public final String message;
    /** Additional metadata about the diagnostic. */
    public final @Nullable List<Integer> tags;
    // /**
    //  * An array of related diagnostic information, e.g. when symbol-names within
    //  * a scope collide all definitions can be marked via this property.
    //  */
    // public @Nullable List<DiagnosticRelatedInformation> relatedInformation;

    /**
     * Create a Diagnostic using the two required arguments.
     *
     * @param range the range at which the message applies
     * @param message the diagnostic's message
     */
    public Diagnostic(Range range, String message) {
        this(range, null, null, null, message, null);
    }

    /**
     * Create a Diagnostic using all arguments.
     *
     * @param range the range at which the message applies
     * @param severity the diagnostic's severity
     * @param code the diagnostic's code, which might appear in the user interface
     * @param source A human-readable string describing the source of this diagnostic, e.g.
     *     'typescript' or 'super lint'.
     * @param message the diagnostic's message
     * @param tags additional metadata about the diagnostic
     */
    public Diagnostic(
            Range range,
            @Nullable Integer severity,
            @Nullable String code,
            @Nullable String source,
            String message,
            @Nullable List<Integer> tags) {
        this.range = range;
        this.severity = severity;
        this.code = code;
        this.source = source;
        this.message = message;
        this.tags = (tags == null ? null : new ArrayList<>(tags));
    }
}
