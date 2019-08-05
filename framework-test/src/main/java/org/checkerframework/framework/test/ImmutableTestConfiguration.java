package org.checkerframework.framework.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.javacutil.PluginUtil;

/**
 * Represents all of the information needed to execute the Javac compiler for a given set of test
 * files.
 */
public class ImmutableTestConfiguration implements TestConfiguration {

    /**
     * Options that should be passed to the compiler. This a {@code Map(optionName &rarr;
     * optionArgumentIfAny)}. E.g.,
     *
     * <pre>{@code
     * Map(
     *   "-AprintAllQualifiers" &rArr; null
     *    "-classpath" &rArr; "myDir1:myDir2"
     * )
     * }</pre>
     */
    private final Map<String, String> options;
    /**
     * These files contain diagnostics that should be returned by Javac. If this list is empty, the
     * diagnostics are instead read from comments in the Java file itself
     */
    private final List<File> diagnosticFiles;

    /**
     * The source files to compile. If the file is expected to emit errors on compilation, the file
     * should contain expected error diagnostics OR should have a companion file with the same
     * path/name but with the extension .out instead of .java if they
     */
    private final List<File> testSourceFiles;

    /** A list of AnnotationProcessors (usually checkers) to pass to the compiler for this test. */
    private final List<String> processors;

    private final boolean shouldEmitDebugInfo;

    public ImmutableTestConfiguration(
            List<File> diagnosticFiles,
            List<File> testSourceFiles,
            List<String> processors,
            Map<String, String> options,
            boolean shouldEmitDebugInfo) {
        this.diagnosticFiles = Collections.unmodifiableList(diagnosticFiles);
        this.testSourceFiles = Collections.unmodifiableList(new ArrayList<>(testSourceFiles));
        this.processors = Collections.unmodifiableList(new ArrayList<>(processors));
        this.options = Collections.unmodifiableMap(new LinkedHashMap<>(options));
        this.shouldEmitDebugInfo = shouldEmitDebugInfo;
    }

    @Override
    public List<File> getTestSourceFiles() {
        return testSourceFiles;
    }

    @Override
    public List<File> getDiagnosticFiles() {
        return diagnosticFiles;
    }

    @Override
    public List<String> getProcessors() {
        return processors;
    }

    @Override
    public Map<String, String> getOptions() {
        return options;
    }

    @Override
    public List<String> getFlatOptions() {
        return TestUtilities.optionMapToList(options);
    }

    @Override
    public boolean shouldEmitDebugInfo() {
        return shouldEmitDebugInfo;
    }

    @Override
    public String toString() {
        return "TestConfigurationBuilder:\n"
                + "testSourceFiles="
                + (testSourceFiles == null ? "null" : PluginUtil.join(" ", testSourceFiles))
                + "\n"
                + "processors="
                + (processors == null ? "null" : PluginUtil.join(", ", processors))
                + "\n"
                + "options="
                + (options == null ? "null" : PluginUtil.join(", ", getFlatOptions()))
                + "\n"
                + "shouldEmitDebugInfo="
                + shouldEmitDebugInfo;
    }
}
