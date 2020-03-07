package org.checkerframework.framework.source.json;

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Diagnostic {
    public final Range range;
    public final @Nullable Integer severity;
    public final @Nullable String code;
    public final @Nullable String source;
    public final String message;
    public final @Nullable List<Integer> tags;
    // public @Nullable List<DiagnosticRelatedInformation> relatedInformation;

    /** Create a Diagnostic using the two required arguments. */
    public Diagnostic(Range range, String message) {
        this(range, null, null, null, message, null);
    }

    /** Create a Diagnostic using all arguments. */
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
