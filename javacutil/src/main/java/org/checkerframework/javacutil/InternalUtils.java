package org.checkerframework.javacutil;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import javax.annotation.processing.ProcessingEnvironment;

/** Miscellaneous static utility methods. */
public class InternalUtils {

    // Class cannot be instantiated.
    private InternalUtils() {
        throw new AssertionError("Class InternalUtils cannot be instantiated.");
    }

    /**
     * Helper function to extract the javac Context from the javac processing environment.
     *
     * @param env the processing environment
     * @return the javac Context
     */
    public static Context getJavacContext(ProcessingEnvironment env) {
        return ((JavacProcessingEnvironment) env).getContext();
    }

    /**
     * Obtain the class loader for {@code clazz}. If that is not available, return the system class
     * loader.
     *
     * @param clazz the class whose class loader to find
     * @return the class loader used to {@code clazz}, or the system class loader, or null if both
     *     are unavailable
     */
    public static ClassLoader getClassLoaderForClass(Class<? extends Object> clazz) {
        ClassLoader classLoader = clazz.getClassLoader();
        return classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader;
    }

    /**
     * Compares tree1 to tree2 by the position at which a diagnostic (e.g., an error message) for
     * the tree should be printed.
     */
    public static int compareDiagnosticPosition(Tree tree1, Tree tree2) {
        DiagnosticPosition pos1 = (DiagnosticPosition) tree1;
        DiagnosticPosition pos2 = (DiagnosticPosition) tree2;

        int preferred = Integer.compare(pos1.getPreferredPosition(), pos2.getPreferredPosition());
        if (preferred != 0) {
            return preferred;
        }

        return Integer.compare(pos1.getStartPosition(), pos2.getStartPosition());
    }
}
