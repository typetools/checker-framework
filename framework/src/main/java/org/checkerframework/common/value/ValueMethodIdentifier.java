package org.checkerframework.common.value;

import com.sun.source.tree.Tree;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.javacutil.TreeUtils;

/** Stores methods that have special handling in the value checker. */
class ValueMethodIdentifier {

    /** String.length() method. */
    private final ExecutableElement lengthMethod;
    /** String.startsWith(String) method. */
    private final ExecutableElement startsWithMethod;
    /** String.endsWith(String) method. */
    private final ExecutableElement endsWithMethod;

    public ValueMethodIdentifier(ProcessingEnvironment processingEnv) {
        lengthMethod = TreeUtils.getMethod("java.lang.String", "length", 0, processingEnv);
        startsWithMethod = TreeUtils.getMethod("java.lang.String", "startsWith", 1, processingEnv);
        endsWithMethod = TreeUtils.getMethod("java.lang.String", "endsWith", 1, processingEnv);
    }

    /** Determines whether a tree is an invocation of the {@code String.length()} method. */
    public boolean isStringLengthInvocation(Tree tree, ProcessingEnvironment processingEnv) {
        return TreeUtils.isMethodInvocation(tree, lengthMethod, processingEnv);
    }

    /** Determines whether a method is the {@code String.length()} method. */
    public boolean isStringLengthMethod(ExecutableElement method) {
        // equals (rather than ElementUtils.ismethod) because String.length cannot be overridden
        return method.equals(lengthMethod);
    }

    /** Determines whether a method is the {@code String.startsWith(String)} method. */
    public boolean isStartsWithMethod(ExecutableElement method) {
        // equals (rather than ElementUtils.ismethod) because String.length cannot be overridden
        return method.equals(startsWithMethod);
    }
    /** Determines whether a method is the {@code String.endsWith(String)} method. */
    public boolean isEndsWithMethod(ExecutableElement method) {
        // equals (rather than ElementUtils.ismethod) because String.length cannot be overridden
        return method.equals(endsWithMethod);
    }
}
