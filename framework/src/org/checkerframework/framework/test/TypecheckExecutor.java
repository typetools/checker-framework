package org.checkerframework.framework.test;


import org.checkerframework.framework.test.diagnostics.JavaDiagnosticReader;
import org.checkerframework.framework.test.diagnostics.TestDiagnostic;
import org.checkerframework.framework.util.PluginUtil;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Used by the Checker Framework test suite to run the framework and generate a test result.
 */
public class TypecheckExecutor {


    public TypecheckExecutor() {
    }

    /**
     * Runs a typechecking test using the given configuration and returns the
     * test result
     */
    public TypecheckResult runTest(TestConfiguration configuration) {
        CompilationResult result = compile(configuration);
        return interpretResults(configuration, result);
    }

    /**
     * Using the settings from the input configuration, compile all source files in the configuration,
     * and return place the result in a CompilationResult
     */
    public CompilationResult compile(TestConfiguration configuration) {
        TestUtilities.ensureDirectoryExists(new File(configuration.getOptions().get("-d")));

        final StringWriter javacOutput = new StringWriter();
        DiagnosticCollector<JavaFileObject> diagnostics = new
                DiagnosticCollector<JavaFileObject>();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        Iterable<? extends JavaFileObject> javaFiles =
                fileManager.getJavaFileObjects(configuration.getTestSourceFiles().toArray(new File[]{}));



        //Even though the getTask takes a list of processors, it fails with a message:
        //error: Class names, 'org.checkerframework.checker.interning.InterningChecker', are only accepted if annotation processing is explicitly requested
        //if the processors are passed this way.  Therefore, we now add them to the beginning of the options list
        final List<String> options = new ArrayList<String>();
        options.add("-processor");
        options.add(PluginUtil.join(",", configuration.getProcessors()));
        options.addAll(configuration.getFlatOptions());

        if (configuration.shouldEmitDebugInfo()) {
            System.out.println("Running test using the following invocation:");
            System.out.println("javac " + PluginUtil.join(" ", options) + " "
                    + PluginUtil.join(" ", configuration.getTestSourceFiles()));
        }

        JavaCompiler.CompilationTask task =
                compiler.getTask(javacOutput, fileManager, diagnostics, options, new ArrayList<String>(), javaFiles);

        /*
         * In Eclipse, std out and std err for multiple tests appear as one
         * long stream. When selecting a specific failed test, one sees the
         * expected/unexpected messages, but not the std out/err messages from
         * that particular test. Can we improve this somehow?
         */
        final Boolean compiledWithoutError = task.call();
        javacOutput.flush();
              return new CompilationResult(compiledWithoutError, javacOutput.toString(), javaFiles,
                                     diagnostics.getDiagnostics());
    }

    /**
     * Reads the expected diagnostics for the given configuration and creates a TypecheckResult
     * which contains all of the missing and expected diagnostics
     */
    public TypecheckResult interpretResults(TestConfiguration config, CompilationResult compilationResult) {
        List<TestDiagnostic> expectedDiagnostics;
        if (config.getDiagnosticFiles() == null || config.getDiagnosticFiles().isEmpty()) {
            expectedDiagnostics = JavaDiagnosticReader.readExpectedDiagnosticsJfo(compilationResult.getJavaFileObjects(), true);
        } else {
            expectedDiagnostics = JavaDiagnosticReader.readDiagnosticFiles(config.getDiagnosticFiles(), true);
        }

        return TypecheckResult.fromCompilationResults(config, compilationResult, expectedDiagnostics);
    }
}
