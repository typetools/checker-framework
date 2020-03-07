package org.checkerframework.framework.source.json;

import java.net.URI;
import java.util.List;

public class PublishDiagnosticsParams {
    // TODO: can these fields be final?
    /** The URI for which diagnostic information is reported. */
    public final URI uri;
    /** Diagnostic information items. */
    public final List<Diagnostic> diagnostics;

    /**
     * Create a PublishDiagnosticsParams with the given arguments.
     *
     * @param uri the URI for which diagnostic information is reported
     * @param diagnostics diagnostic information items
     */
    public PublishDiagnosticsParams(URI uri, List<Diagnostic> diagnostics) {
        this.uri = uri;
        this.diagnostics = diagnostics;
    }
}
