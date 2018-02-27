package org.checkerframework.checker.optional;

import java.util.HashMap;
import java.util.Map;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.TreeUtils;

/** Utility methods used by OptionalVisitor, OptionalAnnotatedTypeFactory, etc. */
public class OptionalUtils {

    // Maps method name to ElecutableElement.  Ignores number of parameters.
    private static Map<String, ExecutableElement> methods =
            new HashMap<String, ExecutableElement>(7); // the Optional class defines 7 methods.

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
}
