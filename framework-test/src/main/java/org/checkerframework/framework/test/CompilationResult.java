package org.checkerframework.framework.test;

import java.util.Collections;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/** CompilationResult represents the output of the compiler after it is run. */
public class CompilationResult {
    private final boolean compiledWithoutError;
    private final String javacOutput;
    private final Iterable<? extends JavaFileObject> javaFileObjects;
    private final List<Diagnostic<? extends JavaFileObject>> diagnostics;

    CompilationResult(
            boolean compiledWithoutError,
            String javacOutput,
            Iterable<? extends JavaFileObject> javaFileObjects,
            List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        this.compiledWithoutError = compiledWithoutError;
        this.javacOutput = javacOutput;
        this.javaFileObjects = javaFileObjects;
        this.diagnostics = Collections.unmodifiableList(diagnostics);
    }

    /**
     * Returns whether or not compilation succeeded without errors or exceptions.
     *
     * @return whether or not compilation succeeded without errors or exceptions
     */
    public boolean compiledWithoutError() {
        return compiledWithoutError;
    }

    /**
     * Returns all of the output from the compiler.
     *
     * @return all of the output from the compiler
     */
    public String getJavacOutput() {
        return javacOutput;
    }

    /**
     * Returns the list of Java files passed to the compiler.
     *
     * @return the list of Java files passed to the compiler
     */
    public Iterable<? extends JavaFileObject> getJavaFileObjects() {
        return javaFileObjects;
    }

    /**
     * Returns the diagnostics reported by the compiler.
     *
     * @return the diagnostics reported by the compiler
     */
    public List<Diagnostic<? extends JavaFileObject>> getDiagnostics() {
        return diagnostics;
    }
}
