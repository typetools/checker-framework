package org.checkerframework.framework.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.plumelib.util.StringsPlume;

/**
 * Represents all of the information needed to execute the Javac compiler for a given set of test
 * files.
 */
public class ImmutableTestConfiguration implements TestConfiguration {

    /**
     * Options that should be passed to the compiler. This a {@code Map(optionName =>
     * optionArgumentIfAny)}. E.g.,
     *
     * <pre>{@code
     * Map(
     *   "-AprintAllQualifiers" => null
     *    "-classpath" => "myDir1:myDir2"
     * )
     * }</pre>
     */
    private final Map<String, @Nullable String> options;
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
    private final List<@BinaryName String> processors;

    /** The value of system property "emit.test.debug". */
    private final boolean shouldEmitDebugInfo;

    /**
     * Create a new ImmutableTestConfiguration.
     *
     * @param diagnosticFiles files containing diagnostics that should be returned by javac
     * @param testSourceFiles the source files to compile
     * @param processors the annotation processors (usually checkers) to run
     * @param options options that should be passed to the compiler
     * @param shouldEmitDebugInfo the value of system property "emit.test.debug"
     */
    public ImmutableTestConfiguration(
            List<File> diagnosticFiles,
            List<File> testSourceFiles,
            List<@BinaryName String> processors,
            Map<String, @Nullable String> options,
            boolean shouldEmitDebugInfo) {
        this.diagnosticFiles = Collections.unmodifiableList(diagnosticFiles);
        this.testSourceFiles = Collections.unmodifiableList(new ArrayList<>(testSourceFiles));
        this.processors = new ArrayList<>(processors);
        this.options =
                Collections.unmodifiableMap(new LinkedHashMap<String, @Nullable String>(options));
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
    public List<@BinaryName String> getProcessors() {
        return processors;
    }

    @Override
    public Map<String, @Nullable String> getOptions() {
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
        return StringsPlume.joinLines(
                "TestConfigurationBuilder:",
                "testSourceFiles=" + StringsPlume.join(" ", testSourceFiles),
                "processors=" + String.join(", ", processors),
                "options=" + String.join(", ", getFlatOptions()),
                "shouldEmitDebugInfo=" + shouldEmitDebugInfo);
    }
}
