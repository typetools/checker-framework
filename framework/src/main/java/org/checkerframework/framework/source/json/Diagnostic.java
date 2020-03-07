package org.checkerframework.framework.source.json;

import java.util.List;

public class Diagnostic {
    public Range range;
    public Integer severity;
    public String code;
    public String source;
    public String message;
    public List<Integer> tags; // DiagnosticTag
}
