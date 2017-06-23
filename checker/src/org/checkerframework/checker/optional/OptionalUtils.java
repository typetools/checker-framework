package org.checkerframework.checker.optional;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import java.util.HashMap;
import java.util.Map;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.TreeUtils;

/** Utility methods used by OptionalVisitor, OptionalAnnotatedTypeFactory, etc. */
public class OptionalUtils {

    // Ignores number of parameters.
    private static Map<String, ExecutableElement> methods =
            new HashMap<String, ExecutableElement>(7);

    private static ExecutableElement getOptionalMethod(
            String methodName, int params, AnnotatedTypeFactory aTypeFactory) {
        ExecutableElement result = methods.get(methodName);
        if (result == null) {
            if (aTypeFactory.getElementUtils().getTypeElement("java.util.Optional") == null) {
                ErrorReporter.errorAbort("The Optional Checker requires Java 8.");
            }
            result =
                    TreeUtils.getMethod(
                            "java.util.Optional",
                            methodName,
                            params,
                            aTypeFactory.getProcessingEnv());
            methods.put(methodName, result);
        }
        return result;
    }

    /**
     * Returns true if the given element is an invocation of the method (in the Optional class), or
     * of any method that overrides that one.
     *
     * <p>Avoids creating the method unless necessary, because the method can only be created on a
     * Java 8 JVM.
     */
    public static boolean isMethodInvocation(
            Tree tree, String name, int params, AnnotatedTypeFactory aTypeFactory) {
        if (!(tree instanceof MethodInvocationTree)) {
            return false;
        }
        MethodInvocationTree methInvok = (MethodInvocationTree) tree;
        ExecutableElement invoked = TreeUtils.elementFromUse(methInvok);
        if (!invoked.getSimpleName().toString().equals(name)) {
            return false;
        }
        ExecutableElement goal = getOptionalMethod(name, params, aTypeFactory);
        return ElementUtils.isMethod(invoked, goal, aTypeFactory.getProcessingEnv());
    }
}
